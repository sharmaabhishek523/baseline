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

package com.aerofs.baseline.admin;

import com.aerofs.baseline.RootEnvironment;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Root resource class for interrogating a baseline
 * service on its overall status.
 * <p>
 * A baseline service may implement zero or more health checks,
 * each of which checks the condition of one or more service
 * components. Each health check returns a {@link HealthCheckStatus.Status}
 * of {@code SUCCESS} or {@code FAILURE}. If all implemented checks
 * return {@code SUCCESS} an HTTP response with status code {@code 200}
 * is returned. If <strong>any</strong> health check returns
 * {@code FAILURE} an HTTP response with status code {@code 503} is
 * returned. Additionally, a JSON object of type {@link ServiceStatus}
 * is returned, detailing all the health checks run, their execution
 * status and any additional information.
 * <p>
 * If <strong>no</strong> health checks are implemented an
 * HTTP response with status code {@code 501} and an empty entity
 * is returned.
 * <p>
 * This resource is accessed via:
 * <pre>
 *     curl http://service_url:service_admin_port/status
 * </pre>
 */
@Path("/status")
@Singleton
@ThreadSafe
public final class HealthCheckResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckResource.class);

    private final RootEnvironment root;
    private final ImmutableMap<String, Class<HealthCheck>> healthChecks;

    // unfortunately I have to use a raw ImmutableMap as
    // a constructor parameter because as far as I understand
    // CDI does not allow you to inject bounded types
    // see http://stackoverflow.com/questions/23992714/how-to-inject-an-unbound-generic-type-in-hk2
    @SuppressWarnings({"rawtypes", "unchecked", "unused"})
    @Inject
    private HealthCheckResource(RootEnvironment root, @Named("healthChecks") ImmutableMap healthChecks) {
        this.root = root;
        this.healthChecks = healthChecks;
    }

    @GET
    public Response getStatus() {
        if (healthChecks.isEmpty()) {
            return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        }

        boolean allSucceeded = true;
        ServiceStatus serviceStatus = new ServiceStatus();

        for (Map.Entry<String, Class<HealthCheck>> entry: healthChecks.entrySet()) {
            String name = entry.getKey();
            try {
                HealthCheck healthCheck = root.getOrCreateInstance(entry.getValue());

                HealthCheckStatus status = healthCheck.check();
                LOGGER.debug("executed \"{}\" with status {}", name, status.getStatus());

                serviceStatus.addStatus(name, status);
                if (status.getStatus() != HealthCheckStatus.Status.SUCCESS) {
                    allSucceeded = false;
                }
            } catch (Exception e) {
                LOGGER.error("failed while executing \"{}\"", name, e);
                allSucceeded = false;
                serviceStatus.addStatus(name, HealthCheckStatus.failure(e.getMessage()));
            }
        }

        return Response
                .status(allSucceeded ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(serviceStatus)
                .build();
    }
}
