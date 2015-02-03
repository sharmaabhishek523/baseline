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

import com.google.common.base.Preconditions;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Singleton;

/**
 * Utility methods for HK2 injection.
 */
public abstract class Injection {

    /**
     * Get the {@link ServiceHandle} to a <strong>singleton</strong>
     * instance of type {@code T}.
     *
     * @param locator {@code ServiceLocator} instance used to locate the singleton instance of type {@code T}
     * @param singletonClass type of the service to be located
     * @param <T> type-parameter identifying the type of the service to be located
     * @return valid {@code ServiceHandle} that points to a <strong>active</strong> singleton instance of type {@code T}
     *
     * @throws IllegalStateException if <strong>no singleton</strong> instance satisfying {@code T} could be found
     */
    public static <T> ServiceHandle<T> getSingletonServiceHandle(ServiceLocator locator, Class<T> singletonClass) {
        ServiceHandle<T> handle = locator.getServiceHandle(singletonClass);

        Preconditions.checkState(handle != null, "fail locate managed type %s", singletonClass.getSimpleName());
        Preconditions.checkState(handle.getActiveDescriptor().getScopeAnnotation() == Singleton.class, "managed type %s is not a singleton", singletonClass.getSimpleName());
        Preconditions.checkState(handle.getService() != null, "fail create managed instance of %s", singletonClass.getSimpleName());
        Preconditions.checkState(handle.isActive(), "%s is no longer active", singletonClass.getSimpleName());

        return handle;
    }

    private Injection() {
        // to prevent instantiation by subclasses
    }
}
