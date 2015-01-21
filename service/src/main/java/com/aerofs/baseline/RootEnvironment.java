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

import com.aerofs.baseline.auth.Authenticator;
import com.aerofs.baseline.auth.Authenticators;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.Timer;
import org.glassfish.hk2.utilities.Binder;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.Validator;
import java.lang.reflect.Type;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Represents the root environment, i.e. the environment
 * underlying both {@link com.aerofs.baseline.AdminEnvironment} and
 * {@link com.aerofs.baseline.ServiceEnvironment}.
 * <br>
 * Changes made to this class are reflected in the
 * underlying environments. Specifically:
 * <ul>
 *     <li>Changes to the <pre>ObjectMapper</pre> are shared between admin and service.</li>
 *     <li>Injectable instances and implementation types added here are available to both admin and service.</li>
 * </ul>
 * <br>
 * Implementation note: This class acts as a facade over
 * {@link com.aerofs.baseline.Injector} and {@link com.aerofs.baseline.LifecycleManager}.
 */
@NotThreadSafe
public final class RootEnvironment {

    private final Injector injector;
    private final LifecycleManager lifecycle;
    private final Validator validator;
    private final ObjectMapper mapper;
    private final Authenticators authenticators;

    RootEnvironment(Injector injector, LifecycleManager lifecycle, Validator validator, ObjectMapper mapper, Authenticators authenticators) {
        this.injector = injector;
        this.lifecycle = lifecycle;
        this.validator = validator;
        this.mapper = mapper;
        this.authenticators = authenticators;
    }

    // FIXME (AG): inject authenticator classes (do after I rethink injection)

    public void addAuthenticator(Authenticator authenticator) {
        authenticators.add(authenticator);
    }

    /**
     * @return baseline-wide javax Validator (shared between admin and service)
     */
    public Validator getValidator() {
        return validator;
    }

    /**
     * @return baseline-wide Jackson JSON object mapper (shared between admin and service)
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * @return baseline-wide scheduled executor instance (shared between admin and service)
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        return lifecycle.getScheduledExecutorService();
    }

    /**
     * @return baseline-wide timer instance (shared between admin and service)
     */
    public Timer getTimer() {
        return lifecycle.getTimer();
    }

    public void addManaged(Managed managed) {
        lifecycle.addManaged(managed);
    }

    public <T> T getOrCreateInstance(Class<T> requested) {
        return injector.getOrCreateInstance(requested);
    }

    public void addInjectableNamedConstant(String name, Object instance) {
        injector.addInjectableNamedConstant(name, instance);
    }

    public void addInjectableSingletonInstance(Object instance, Type... additionalExportedInterfaces) {
        injector.addInjectableSingletonInstance(instance, additionalExportedInterfaces);
    }

    public void addInjectableImplementationType(Class<?> type) {
        injector.addInjectableImplementationType(type);
    }

    public void addInjectable(Binder binder) {
        injector.addInjectable(binder);
    }
}
