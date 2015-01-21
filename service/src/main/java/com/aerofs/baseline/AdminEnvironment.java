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

import com.aerofs.baseline.admin.Command;
import com.aerofs.baseline.admin.CommandsResource;
import com.aerofs.baseline.admin.HealthCheck;
import com.aerofs.baseline.admin.HealthCheckResource;
import com.aerofs.baseline.admin.InvalidCommandExceptionMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Map;

@NotThreadSafe
public final class AdminEnvironment extends ApplicationEnvironment {

    private final Map<String, Class<? extends Command>> commands = Maps.newHashMap();
    private final Map<String, Class<? extends HealthCheck>> healthChecks = Maps.newHashMap();

    AdminEnvironment(String name) {
        super(name);

        // add exception mappers that only apply to the admin environment
        resourceConfig.register(InvalidCommandExceptionMapper.class);

        // configure the injector
        resourceConfig.register(new AbstractBinder() {

            // this method is only called when we initialize
            // and start the admin service. by that point all
            // commands and health checks should be
            // registered, and we make an immutable copy of them
            // so that they can be iterated over by the
            // resource classes. we *cannot* have these as
            // "private final" fields because they will be instantiated
            // early using empty maps
            @Override
            protected void configure() {
                bind(ImmutableMap.copyOf(commands)).to(ImmutableMap.class).named("commands");
                bind(ImmutableMap.copyOf(healthChecks)).to(ImmutableMap.class).named("healthChecks");
            }
        });

        // add the resources we expose via the admin api
        resourceConfig.register(CommandsResource.class);
        resourceConfig.register(HealthCheckResource.class);
    }

    public void registerCommand(String name, Class<? extends Command> command) {
        Object previous = commands.putIfAbsent(name, command);
        Preconditions.checkState(previous == null, "previously registered command \"%s\"", name);
    }

    public void registerHealthCheck(String name, Class<? extends HealthCheck> healthCheck) {
        Object previous = healthChecks.putIfAbsent(name, healthCheck);
        Preconditions.checkState(previous == null, "previously registered health check \"%s\"", name);
    }
}
