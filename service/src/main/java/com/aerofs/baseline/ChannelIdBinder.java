package com.aerofs.baseline;

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

import com.aerofs.baseline.http.ChannelId;
import com.aerofs.baseline.http.RequestProperties;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.annotation.Nullable;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

/**
 * Implementation of an HK2 {@code Binder} that injects
 * {@link ChannelId} instances into {@link javax.ws.rs.core.Context}-
 * annotated JAX-RS objects and methods.
 * <br>
 * An instance of this class should <strong>only</strong> be added
 * as a JAX-RS {@link javax.ws.rs.ext.Provider} to a Jersey application.
 * <br>
 * This implementation depends on the {@link ContainerRequestContext
 * injector defined by Jersey and which is automatically added
 * to Jersey applications.
 */
final class ChannelIdBinder extends AbstractBinder {

    private static final class ChannelIdFactory implements Factory<ChannelId> {

        private final @Nullable ChannelId id;

        private ChannelIdFactory(@Context ContainerRequestContext context) {
            Object property = context.getProperty(RequestProperties.REQUEST_CONTEXT_CHANNEL_ID_PROPERTY);
            this.id = property == null ? null : (ChannelId) property;
        }

        @Override
        public @Nullable ChannelId provide() {
            return id;
        }

        @Override
        public void dispose(ChannelId instance) {
            // noop
        }
    }

    @Override
    protected void configure() {
        bindFactory(ChannelIdFactory.class).to(ChannelId.class);
    }
}
