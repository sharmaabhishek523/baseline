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

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class ServiceEnvironment extends ApplicationEnvironment {

    ServiceEnvironment(String name) {
        super(name);

        // setup JAX-RS role-based authorization
        resourceConfig.register(RolesAllowedDynamicFeature.class);
    }
}
