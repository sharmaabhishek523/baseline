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

import com.aerofs.baseline.AdminEnvironment;
import com.aerofs.baseline.RootEnvironment;
import com.aerofs.baseline.Service;
import com.aerofs.baseline.ServiceConfiguration;
import com.aerofs.baseline.ServiceEnvironment;
import com.aerofs.baseline.http.HttpClientResource;
import com.aerofs.baseline.http.HttpUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public final class TestAuth {

    private static final Set<String> ROOT_ROLES = ImmutableSet.of("admin", "user");
    private static final Set<String> USER_ROLES = ImmutableSet.of("user");

    //
    // Throwing authenticator
    //

    public static final class ThrowingAuthenticator implements Authenticator {

        @Override
        public String getName() {
            return "THROW";
        }

        @Override
        public AuthenticationResult authenticate(MultivaluedMap<String, String> headers) {
            List<String> results = headers.get("Throw");

            if (results == null) {
                return AuthenticationResult.UNSUPPORTED;
            }

            if (results.size() != 1) {
                return AuthenticationResult.FAILED;
            }

            throw new RuntimeException("THROW HARD");
        }
    }

    //
    // Custom "HeFancy" Authentication
    //

    public static final class HeFancySecurityContext implements SecurityContext {

        public static final String HE_FANCY = "HE_FANCY";

        private final Principal principal;
        private final Set<String> allowedRoles;

        public HeFancySecurityContext(String user, Set<String> allowedRoles) {
            this.principal = () -> user;
            this.allowedRoles = allowedRoles;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            return allowedRoles.contains(role);
        }

        @Override
        public boolean isSecure() {
            return true; // so true...
        }

        @Override
        public String getAuthenticationScheme() {
            return HE_FANCY;
        }
    }

    public static final class HeFancyAuthenticator implements Authenticator {

        private static final String FANCY_AUTHENTICATION = "Fancy-Authentication";
        private static final String USER = "ABHISHEK";
        private static final String PASS = "PASSWORD";

        @Override
        public String getName() {
            return HeFancySecurityContext.HE_FANCY;
        }

        @Override
        public AuthenticationResult authenticate(MultivaluedMap<String, String> headers) {
            List<String> results = headers.get(FANCY_AUTHENTICATION);

            if (results == null) {
                return AuthenticationResult.UNSUPPORTED;
            }

            if (results.size() != 1) {
                return AuthenticationResult.FAILED;
            }

            Preconditions.checkArgument(results.size() == 1);
            String authorization = results.get(0);

            // suspiciously similar to http basic authentication
            // coincidence?
            // YES
            authorization = authorization.replaceAll("[Ff]ancy", "");
            authorization = authorization.trim();

            byte[] decoded = BaseEncoding.base64().decode(authorization);
            String authString = new String(decoded, Charsets.US_ASCII);

            String[] authComponents = authString.split(":");

            if (authComponents.length == 2) {
                if (authComponents[0].equals(USER) && authComponents[1].equals(PASS)) {
                    return new AuthenticationResult(AuthenticationResult.Status.SUCCEEDED, new HeFancySecurityContext(USER, USER_ROLES));
                }
            }

            return AuthenticationResult.FAILED;
        }
    }

    //
    // HTTP Basic Authentication
    //

    public static final class BasicSecurityContext implements SecurityContext {

        private final Principal principal;
        private final Set<String> allowedRoles;

        public BasicSecurityContext(String user, Set<String> allowedRoles) {
            this.principal = () -> user;
            this.allowedRoles = allowedRoles;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            return allowedRoles.contains(role);
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getAuthenticationScheme() {
            return SecurityContext.BASIC_AUTH;
        }
    }

    public static final class HttpBasicAuthenticator implements Authenticator {

        private static final String USER = "USER";
        private static final String ROOT = "ROOT";
        private static final String PASS = "PASS";

        @Override
        public String getName() {
            return SecurityContext.BASIC_AUTH;
        }

        @Override
        public AuthenticationResult authenticate(MultivaluedMap<String, String> headers) {
            List<String> results = headers.get(HttpHeaders.AUTHORIZATION);

            if (results == null) {
                return AuthenticationResult.UNSUPPORTED;
            }

            if (results.size() != 1) {
                return AuthenticationResult.FAILED;
            }

            Preconditions.checkArgument(results.size() == 1);
            String authorization = results.get(0);

            authorization = authorization.replaceAll("[Bb]asic", "");
            authorization = authorization.trim();

            byte[] decoded = BaseEncoding.base64().decode(authorization);
            String authString = new String(decoded, Charsets.US_ASCII);

            String[] authComponents = authString.split(":");

            if (authComponents.length == 2) {
                if (authComponents[0].equals(USER) && authComponents[1].equals(PASS)) {
                    return new AuthenticationResult(AuthenticationResult.Status.SUCCEEDED, new BasicSecurityContext(USER, USER_ROLES));
                } else if (authComponents[0].equals(ROOT) && authComponents[1].equals(PASS)) {
                    return new AuthenticationResult(AuthenticationResult.Status.SUCCEEDED, new BasicSecurityContext(USER, ROOT_ROLES));
                }
            }

            return AuthenticationResult.FAILED;
        }
    }

    //
    // service definition
    //

    @Path("/restricted")
    public static final class RootResource {

        private static final String SUPER_SECRET = "super_secret";

        @RolesAllowed("admin")
        @GET
        public String getAdminValue() {
            return SUPER_SECRET;
        }
    }

    @Path("/everyone")
    public static final class UserResource {

        private static final String BORING = "boring";

        @RolesAllowed({"admin", "user"})
        @GET
        public String getAdminValue() {
            return BORING;
        }
    }

    public static final class TestService extends Service<ServiceConfiguration> {

        public TestService() {
            super("test");
        }

        @Override
        public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
            root.addAuthenticator(new HeFancyAuthenticator()); // try fancy authentication first
            root.addAuthenticator(new HttpBasicAuthenticator()); // then basic auth...
            root.addAuthenticator(new ThrowingAuthenticator());

            service.addResource(UserResource.class);
            service.addResource(RootResource.class);
        }
    }

    //
    // tests
    //

    @Rule
    public HttpClientResource client = new HttpClientResource();

    private TestService server;

    @Before
    public void setup() throws Exception {
        server = new TestService();
        server.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);
    }

    @After
    public void teardown() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void shouldRejectUnauthenticatedAccessToRestrictedResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/restricted");

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    public void shouldRejectUnauthenticatedAccessToUserResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/everyone");

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    public void shouldRejectUserAccessToRestrictedResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/restricted");
        request.setHeader("Authorization", "Basic " + getEncodedCredentials(HttpBasicAuthenticator.USER, HttpBasicAuthenticator.PASS));

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    public void shouldAllowAuthorizedUserAccessToUserResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/everyone");
        request.setHeader("Authorization", "Basic " + getEncodedCredentials(HttpBasicAuthenticator.USER, HttpBasicAuthenticator.PASS));

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(HttpUtils.readResponseEntityToString(response), equalTo(UserResource.BORING));
    }

    @Test
    public void shouldAllowAuthorizedRootAccessToUserResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/everyone");
        request.setHeader("Authorization", "Basic " + getEncodedCredentials(HttpBasicAuthenticator.ROOT, HttpBasicAuthenticator.PASS));

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(HttpUtils.readResponseEntityToString(response), equalTo(UserResource.BORING));
    }

    @Test
    public void shouldAllowAuthorizedRootAccessToRestrictedResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/restricted");
        request.setHeader("Authorization", "Basic " + getEncodedCredentials(HttpBasicAuthenticator.ROOT, HttpBasicAuthenticator.PASS));

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(HttpUtils.readResponseEntityToString(response), equalTo(RootResource.SUPER_SECRET));
    }

    @Test
    public void shouldAllowFancyAuthorizedUserAccessToUserResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/everyone");
        request.setHeader("Fancy-Authentication", "Fancy " + getEncodedCredentials(HeFancyAuthenticator.USER, HeFancyAuthenticator.PASS));

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.OK.getStatusCode()));
        assertThat(HttpUtils.readResponseEntityToString(response), equalTo(UserResource.BORING));
    }

    @Test
    public void shouldRejectFancyAuthorizedUserAccessToUserResource() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/restricted");
        request.setHeader("Fancy-Authentication", "Fancy " + getEncodedCredentials(HeFancyAuthenticator.USER, HeFancyAuthenticator.PASS));

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    public void shouldRejectUserAccessWhenFirstAuthenticatorInChainFails() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/everyone");
        request.setHeader("Fancy-Authentication", "Fancy " + getEncodedCredentials("NONE", "NONE"));
        request.setHeader("Authorization", "Basic " + getEncodedCredentials(HttpBasicAuthenticator.USER, HttpBasicAuthenticator.PASS));

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    public void shouldGetInternalServerErrorIfAuthenticatorThrows() throws Exception {
        HttpUriRequest request = new HttpGet(ServiceConfiguration.SERVICE_URL + "/everyone");
        request.setHeader("Throw", "bogus");

        Future<HttpResponse> future = client.getClient().execute(request, null);
        HttpResponse response = future.get();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    private static String getEncodedCredentials(String user, String pass) {
        return BaseEncoding.base64().encode(String.format("%s:%s", user, pass).getBytes(Charsets.US_ASCII));
    }
}
