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

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
abstract class ApplicationEnvironment {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    protected final ResourceConfig resourceConfig = new ResourceConfig();

    protected ApplicationEnvironment(String name) {
        resourceConfig.setApplicationName(name);
    }

    public final void addProperty(String name, Object value) {
        resourceConfig.property(name, value);
    }

    public final void addResource(Class<?> type) {
        resourceConfig.register(type);
    }

    public final void addResource(Object instance) {
        resourceConfig.register(instance);
    }

    public final void addProvider(Class<?> type) {
        resourceConfig.register(type);
    }

    public final void addProvider(Object instance) {
        resourceConfig.register(instance);
    }

    // should only be accessed by the Service class
    final ResourceConfig getResourceConfig() {
        return resourceConfig;
    }
}
