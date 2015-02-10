/*
 * Copyright 2015 Air Computing Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aerofs.baseline;

import com.aerofs.baseline.auth.AuthenticationExceptionMapper;
import com.aerofs.baseline.auth.AuthenticationFilter;
import com.aerofs.baseline.auth.Authenticators;
import com.aerofs.baseline.config.Configuration;
import com.aerofs.baseline.http.HttpConfiguration;
import com.aerofs.baseline.http.HttpServer;
import com.aerofs.baseline.json.JsonProcessingExceptionMapper;
import com.aerofs.baseline.json.ValidatingJacksonJaxbJsonProvider;
import com.aerofs.baseline.logging.Logging;
import com.aerofs.baseline.metrics.MetricRegistries;
import com.aerofs.baseline.metrics.MetricsCommand;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import io.netty.util.Timer;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.hibernate.validator.HibernateValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@NotThreadSafe
public abstract class Service<T extends Configuration> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final AtomicReference<ServiceLocator> rootLocatorReference = new AtomicReference<>(null);
    private final AtomicReference<LifecycleManager> lifecycleReference = new AtomicReference<>(null);
    private final String name;

    protected Service(String name) {
        this.name = name;
    }

    @SuppressWarnings({"unused", "unchecked"})
    public final void run(String[] args) throws Exception {
        // add the uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler());

        // enable all logging to the console
        Logging.setupErrorConsoleLogging();

        // check that the filename is specified
        if (args.length != 1) {
            LOGGER.error("incorrect number of arguments exp:{} act:{}", 1, args.length);
            System.exit(Constants.INVALID_ARGUMENTS_EXIT_CODE);
        }

        // check that it's a YAML file
        String config = args[0];
        if (!config.endsWith(".yml")) {
            LOGGER.error("yaml configuration file with filename ending with .yml expected");
            System.exit(Constants.INVALID_ARGUMENTS_EXIT_CODE);
        }

        // load the yaml config file (don't validate)
        T configuration = null;
        try {
            configuration = Configuration.loadYAMLConfigurationFromFile(getClass(), config);
        } catch (Exception e) {
            LOGGER.error("fail load {}", config, e);
            System.exit(Constants.INVALID_CONFIGURATION_EXIT_CODE);
        }

        // run baseline with the parsed configuration
        try {
            Preconditions.checkArgument(configuration != null, "%s could not be loaded", config);
            runWithConfiguration(configuration);
        } catch (Exception e) {
            LOGGER.error("fail start server", e);
            System.exit(Constants.FAIL_SERVICE_START_EXIT_CODE);
        }
    }

    public final void runWithConfiguration(T configuration) throws Exception {
        // initialize the validation subsystem
        ValidatorFactory validatorFactory = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
        Validator validator = validatorFactory.getValidator();

        // validate the configuration
        Configuration.validateConfiguration(validator, configuration);

        // at this point we have a valid configuration
        // we can start logging to the correct location,
        // initialize common services, and start up
        // the jersey applications

        // reset the logging subsystem with the configured levels
        Logging.setupLogging(configuration.getLogging());

        // display the server banner
        displayBanner();

        // add a shutdown hook to release all resources cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        // setup some default metrics for the JVM
        MetricRegistries.getRegistry().register(Constants.JVM_BUFFERS, new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        MetricRegistries.getRegistry().register(Constants.JVM_GC, new GarbageCollectorMetricSet());
        MetricRegistries.getRegistry().register(Constants.JVM_MEMORY, new MemoryUsageGaugeSet());
        MetricRegistries.getRegistry().register(Constants.JVM_THREADS, new ThreadStatesGaugeSet());

        // create the root service locator
        ServiceLocator rootLocator = ServiceLocatorFactory.getInstance().create("root");
        rootLocatorReference.set(rootLocator);

        // grab a reference to our system-wide class loader
        ClassLoader classLoader = rootLocator.getClass().getClassLoader();

        // no one accesses the locator directly - they go through the injector
        Injector injector = new Injector(rootLocator);

        // create the lifecycle manager
        LifecycleManager lifecycle = new LifecycleManager();
        lifecycleReference.set(lifecycle);

        // after this point any services
        // added to the LifecycleManager will be
        // shut down cleanly

        // create and setup the system-wide Jackson object mapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // create container for user-specified set of request authenticators
        Authenticators authenticators = new Authenticators();

        // create the root environment
        RootEnvironment root = new RootEnvironment(injector, lifecycle, validator, mapper, authenticators);

        // inject system-wide instances
        root.addInjectableNamedConstant(Constants.SERVICE_NAME_INJECTION_KEY, name);
        root.addInjectableSingletonInstance(validator, Validator.class);
        root.addInjectableSingletonInstance(mapper);
        root.addInjectableSingletonInstance(injector);
        root.addInjectableSingletonInstance(root);
        root.addInjectableSingletonInstance(lifecycle.getScheduledExecutorService(), ScheduledExecutorService.class);
        root.addInjectableSingletonInstance(lifecycle.getTimer(), Timer.class); // FIXME (AG): use our own timer interface
        root.addInjectableSingletonInstance(configuration, Configuration.class);
        root.addInjectableSingletonInstance(authenticators);

        // create the two environments (admin and service)
        String adminName = name + "-" + Constants.ADMIN_IDENTIFIER;
        AdminEnvironment admin = new AdminEnvironment(adminName);
        initializeJerseyApplication(adminName, classLoader, validator, mapper, admin);

        String serviceName = name + "-" + Constants.SERVICE_IDENTIFIER;
        ServiceEnvironment service = new ServiceEnvironment(serviceName);
        initializeJerseyApplication(serviceName, classLoader, validator, mapper, service);

        // register some basic admin commands
        admin.registerCommand("gc", GarbageCollectionCommand.class);
        admin.registerCommand("metrics", MetricsCommand.class);

        // punt to subclasses for further configuration
        init(configuration, root, admin, service);

        // after this point we can't add any more jersey providers
        // resources, etc. and we're ready to expose our server to the world

        LOGGER.trace("registered injectables:");
        for (ActiveDescriptor<?> descriptor : injector.getDescriptors()) {
            LOGGER.trace("registered i:{} d:{}", descriptor.getImplementationClass(), descriptor);
        }

        // initialize the admin http server
        HttpConfiguration adminConfiguration = configuration.getAdmin();
        ApplicationHandler adminHandler = new ApplicationHandler(admin.getResourceConfig(), null, rootLocator);
        final HttpServer adminHttpServer = new HttpServer(Constants.ADMIN_IDENTIFIER, adminConfiguration, lifecycle.getTimer(), adminHandler);
        lifecycle.addManaged(adminHttpServer);

        // initialize the service http server
        HttpConfiguration serviceConfiguration = configuration.getService();
        ApplicationHandler serviceHandler = new ApplicationHandler(service.getResourceConfig(), null, rootLocator);
        final HttpServer serviceHttpServer = new HttpServer(Constants.SERVICE_IDENTIFIER, serviceConfiguration, lifecycle.getTimer(), serviceHandler);
        lifecycle.addManaged(serviceHttpServer);

        // finally, start up all managed services (which includes the two servers above)
        lifecycle.start();
    }

    private void initializeJerseyApplication(String applicationName, ClassLoader classLoader, Validator validator, ObjectMapper mapper, ApplicationEnvironment environment) {
        // set our name
        environment.getResourceConfig().setApplicationName(applicationName);

        // set the class-loader to be the system-wide class loader
        environment.getResourceConfig().setClassLoader(classLoader);

        // set default properties
        environment.addProperty(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);
        environment.addProperty(ServerProperties.WADL_FEATURE_DISABLE, true);
        environment.addProperty(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        // add exception mappers
        environment.getResourceConfig().register(DefaultExceptionMapper.class);
        environment.getResourceConfig().register(AuthenticationExceptionMapper.class);
        environment.getResourceConfig().register(IllegalArgumentExceptionMapper.class);
        environment.getResourceConfig().register(JsonProcessingExceptionMapper.class);
        environment.getResourceConfig().register(ConstraintViolationExceptionMapper.class);

        // add other providers
        environment.getResourceConfig().register(AuthenticationFilter.class);
        environment.getResourceConfig().register(RolesAllowedDynamicFeature.class);
        environment.getResourceConfig().register(new ValidatingJacksonJaxbJsonProvider(validator, mapper, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS));
    }

    private void displayBanner() {
        try {
            String banner = Resources.toString(Resources.getResource("banner.txt"), Charsets.UTF_8);
            LOGGER.info("{}", banner);
        } catch (IllegalArgumentException | IOException e) { // happens if banner.txt doesn't exist in classpath
            LOGGER.info("starting {}", name);
        }
    }

    public abstract void init(T configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception;

    public final void shutdown() {
        // stop all managed services
        LifecycleManager lifecycle = lifecycleReference.get();
        if (lifecycle != null) { lifecycle.stop(); }

        // shutdown the injection system
        ServiceLocator locator = rootLocatorReference.get();
        if (locator != null) { locator.shutdown(); }

        // stop recording metrics
        MetricRegistries.unregisterMetrics();

        // turn off logging
        Logging.stopLogging();
    }
}
