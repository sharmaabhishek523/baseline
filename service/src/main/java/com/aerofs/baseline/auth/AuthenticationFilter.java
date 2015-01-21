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

package com.aerofs.baseline.auth;

import com.aerofs.baseline.http.RequestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Implementation of a Jersey {@code ContainerRequestFilter}
 * that processes incoming HTTP requests and updates their associated
 * {@link javax.ws.rs.core.SecurityContext} if necessary.
 *
 * This implementation uses the user-specified {@link com.aerofs.baseline.auth.Authenticator}
 * implementations to authenticate incoming HTTP requests. It iterates
 * through this set until it finds the <strong>first</strong> implementation
 * that can authenticate the request (whether successfully or not).
 */
@ThreadSafe
@Provider
@Priority(Priorities.AUTHENTICATION)
@Singleton
public final class AuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final Authenticators authenticators;

    public AuthenticationFilter(@Context Authenticators authenticators) {
        this.authenticators = authenticators;
    }

    /**
     * Updates the {@link javax.ws.rs.core.SecurityContext} associated
     * with this request if a user-specified {@link com.aerofs.baseline.auth.Authenticator}
     * was able to authenticate the provided credentials.
     *
     * @param requestContext JAX-RS HTTP request context
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityContext securityContext = requestContext.getSecurityContext();

        if (needsAuth(securityContext)) {
            for (Authenticator authenticator : authenticators) {
                try {
                    AuthenticationResult result = authenticator.authenticate(requestContext.getHeaders());
                    if (result.getStatus() == AuthenticationResult.Status.SUCCEEDED) {
                        requestContext.setSecurityContext(result.getSecurityContext()); // override default unauthenticated context
                        break;
                    } else if (result.getStatus() == AuthenticationResult.Status.FAILED) {
                        break;
                    }
                } catch (AuthenticationException e) {
                    logError(requestContext, authenticator, e);
                    throw e;
                } catch (Exception e) {
                    logError(requestContext, authenticator, e);
                    throw new IOException("fail authenticate using " + authenticator.getName(), e);
                }
            }
        }
    }

    private static boolean needsAuth(@Nullable SecurityContext securityContext) {
        return securityContext instanceof UnauthenticatedSecurityContext || securityContext == null || securityContext.getUserPrincipal() == null;
    }

    private static void logError(ContainerRequestContext requestContext, Authenticator authenticator, Exception e) {
        LOGGER.warn("{}: [{}] fail authenticate using authenticator {}", requestContext.getProperty(RequestProperties.REQUEST_CONTEXT_CHANNEL_ID_PROPERTY), requestContext.getProperty(RequestProperties.REQUEST_CONTEXT_REQUEST_ID_PROPERTY), authenticator.getName(), e);
    }
}
