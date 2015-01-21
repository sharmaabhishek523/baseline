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

import com.google.common.collect.Sets;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.reflection.ReflectionHelper;
import org.jvnet.hk2.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Wrapper over an HK2 {@link org.glassfish.hk2.api.ServiceLocator}.
 * <br>
 * Contains convenience methods to:
 * <ul>
 *     <li>Register a named constant with the {@code locator}.</li>
 *     <li>Register a singleton instance with the {@code locator}.</li>
 *     <li>Register a class with the specified types with the {@code locator}.</li>
 *     <li>Get or construct a class instance via {@code locator}.</li>
 * </ul>
 * This injector does <strong>not</strong> have lifecycle semantics -
 * it is simply an operational wrapper. As a result, its lifecycle is bound
 * to that of the underlying {@code ServiceLocator}, and it remains
 * valid as long as the underlying {@code ServiceLocator} has not been shutdown.
 * It is the caller's responsibility to manage and shutdown the underlying
 * {@code ServiceLocator} and prevent this class from being used afterwards.
 */
@ThreadSafe
public final class Injector {

    private static final Logger LOGGER = LoggerFactory.getLogger(Injector.class);

    private final ServiceLocator locator;
    private final Set<Class<?>> foundAndCreatedSingletonCache = Sets.newHashSet(); // protected by this

    /**
     * Constructor.
     *
     * @param locator {@code ServiceLocator} wrapped by this injector
     */
    Injector(ServiceLocator locator) {
        this.locator = locator;
    }

    /**
     * @return list of HK2 injection descriptors currently registered with the wrapped {@link ServiceLocator}
     */
    List<ActiveDescriptor<?>> getDescriptors() {
        return locator.getDescriptors(descriptor -> true);
    }

    // FIXME (AG): investigate why Jersey doesn't have this issue for root-resource classes...
    /**
     * Get an existing object that satisfies the type
     * constraint {@code requested}, or create a new object (if possible)
     * if one does not exist.
     * <br>
     * Implementation note: when this method is used to <strong>create</strong>
     * an object, the created object is <strong>not managed</strong> by
     * HK2. This means that lifetimes (specifically, singleton scopes) are
     * not respected. To work around this, the {@code Injector} maintains
     * a cache of created singleton types, and explicitly binds any singleton
     * instances back into the underlying {@code ServiceLocator}.
     * <br>
     * Due to the use of a shared cache callers may experience lock contention.
     *
     * @param requested type for which the injector should supply an implementation instance
     * @param <T> type parameter constraining the implementation instance
     * @return {@code null} if an object of type {@code T} could not be created,
     * or a valid instance satisfying {@code T}
     */
    public <T> T getOrCreateInstance(Class<T> requested) {
        ServiceHandle<T> handle = locator.getServiceHandle(requested);

        // IMPORTANT: when you use ServiceLocator.createAndInitialize
        // any *created* instances are not managed by the locator.
        // the practical impact of this is that scope lifetimes are
        // not respected. (for example, singletons are created multiple times)
        // to ensure that singletons (at least) remain singletons
        // I bind the created instance to the locator

        if (handle == null) { // wasn't created already
            T returned;

            if (!hasSingletonScope(requested)) { // not a singleton, so we'll create and return it directly
                returned = locator.createAndInitialize(requested);
            }else { // it's a singleton
                // the objective of this entire block is to
                // bind the created instance to the service
                // locator *only once*
                // FIXME (AG): locks for *all* singletons - stripe
                synchronized (this) {
                    returned = locator.createAndInitialize(requested);
                    if (!foundAndCreatedSingletonCache.contains(requested)) {
                        addInjectableSingletonInstance(returned, requested);
                        foundAndCreatedSingletonCache.add(requested);
                        LOGGER.trace("cache singleton instance t:{} h:{}", returned.getClass(), returned.hashCode());
                    }
                }
            }

            return returned;
        } else {
            return handle.getService();
        }
    }

    /**
     * Adds a constant named {@code constanceName} to the
     * {@code ServiceLocator} wrapped by this {@code Injector}.
     * <br>
     * The constant can later be retrieved via an inject call, for example:
     * <pre>
     *      &#64;Inject
     *      &#64;Named(constantName)
     *      private int maxTopicLength;
     * </pre>
     *
     * @param constantName unique name for the constant
     * @param constant non-primitive value of the constant
     */
    public void addInjectableNamedConstant(String constantName, Object constant) {
        ServiceLocatorUtilities.addOneConstant(locator, constant, constantName);
    }

    /**
     * Adds a constructed instance to the {@code ServiceLocator}
     * wrapped by this {@code Injector}.
     * <br>
     * The supplied instance is added to the {@code ServiceLocator}
     * in the singleton scope. There are two ways of using this method,
     * and they configure the {@code ServiceLocator} differently.
     * <ol>
     *     <li>{@code addInjectableSingletonInstance(instance)}: bound to
     *         the {@code ServiceLocator} with the implementation class type
     *         as well as any interfaces annotated with {@code @Contract}.</li>
     *     <li>{@code addInjectableSingletonInstance(instance, Interface1, Interface2)}:
     *         bound to the {@code ServiceLocator} with the implementation
     *         class type, Interface1, Interface2, as well as any interfaces
     *         annotated with {@code @Contract}.</li>
     * </ol>
     * @param singleton instance to be available via the {@code ServiceLocator}
     * @param additionalExportedInterfaces contracts supported by this object. This allows the object
     *                                     to be returned in response to a getOrCreateInstance(Interface1.class)
     *                                     in addition to getOrCreateInstance(Instance.class)
     */
    public void addInjectableSingletonInstance(Object singleton, Type... additionalExportedInterfaces) {
        Set<Type> combined = Sets.newHashSet();

        // add the following contracts:
        // 1. the implementation class itself
        // 2. any caller-specified interfaces, super-classes, etc.
        // 3. any implemented interfaces annotated with @Contract
        Collections.addAll(combined, additionalExportedInterfaces);
        combined.add(singleton.getClass());
        combined.addAll(ReflectionHelper.getAdvertisedTypesFromObject(singleton, Contract.class));

        // create a descriptor and explicitly set it to singleton scope
        Type[] contracts = new Type[combined.size()];
        AbstractActiveDescriptor<Object> descriptor = BuilderHelper.createConstantDescriptor(singleton, null, combined.toArray(contracts));
        descriptor.setScopeAnnotation(Singleton.class);

        // register this instance with the locator
        ServiceLocatorUtilities.addOneDescriptor(locator, descriptor, false);
    }

    /**
     * Add an implementation class type to the {@code ServiceLocator}
     * wrapped by this {@code Injector}.
     * <br>
     * If the class or any interfaces it implements have JSR-339 scope
     * or qualifier annotations they are scanned and respected.
     *
     * @param implementationType class to be added to the {@code ServiceLocator}
     */
    public void addInjectableImplementationType(Class<?> implementationType) {
        ServiceLocatorUtilities.addClasses(locator, implementationType);
    }

    // IMPORTANT: binders are dangerous!
    // They ignore *all* annotations and you have to specify scope, name, etc.

    /**
     * Configure the {@code ServiceLocator} wrapped by this
     * {@code Injector} using a caller-specified {@code Binder}.
     *
     * This is injection configuration in hard mode. When
     * using a {@code binder} all class annotations are ignored
     * and the caller must specify the binding type and scope
     * explicitly. The upside is substantially more flexibility
     * than the convenience methods provided in {@code Injector}.
     * <br>
     * Examples:
     * <br>
     * To bind a constant:
     * <pre>
     *     root.addInjectable(new AbstractBinder() {
     *
     *         void configure() {
     *             bind(constant).to(Constant.class).named("constant_name");
     *         }
     *     }
     * </pre>
     * To bind an already-created singleton instance:
     * <pre>
     *     root.addInjectable(new AbstractBinder() {
     *
     *         void configure() {
     *             bind(instance).to(Instance.class);
     *         }
     *     }
     * </pre>
     * To bind a yet-to-be-created singleton instance:
     * <pre>
     *     root.addInjectable(new AbstractBinder() {
     *
     *         void configure() {
     *             bind(Instance.class).to(Instance.class).in(Singleton.class);
     *         }
     *     }
     * </pre>
     * To bind a per-lookup instance:
     * <pre>
     *     root.addInjectable(new AbstractBinder() {
     *
     *         void configure() {
     *             bind(Instance.class).to(Instance.class);
     *         }
     *     }
     * </pre>
     *
     * @param binder caller-specified injection binder
     */
    public void addInjectable(Binder binder) {
        ServiceLocatorUtilities.bind(locator, binder);
    }

    private static boolean hasSingletonScope(Class<?> type) {
        return type.isAnnotationPresent(Singleton.class);
    }
}
