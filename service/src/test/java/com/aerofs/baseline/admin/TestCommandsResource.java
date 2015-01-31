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

import com.aerofs.baseline.AdminEnvironment;
import com.aerofs.baseline.RootEnvironment;
import com.aerofs.baseline.Service;
import com.aerofs.baseline.ServiceConfiguration;
import com.aerofs.baseline.ServiceEnvironment;
import com.aerofs.baseline.errors.BaseExceptionMapper;
import com.aerofs.baseline.errors.BaselineError;
import com.aerofs.baseline.errors.ServiceError;
import com.aerofs.baseline.http.HttpClientResource;
import com.aerofs.baseline.http.HttpUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public final class TestCommandsResource {

    //
    // NOTE: classes have to be public static
    // for hk2 to be able to instantiate them
    //

    public static final class SuccessfulNonEmptyEntityCommand implements Command {

        @Override
        public void execute(MultivaluedMap<String, String> queryParameters, PrintWriter entityWriter) throws Exception {
            entityWriter.write("EXECUTED");
        }
    }

    public static final class SuccessfulJsonEntityCommand implements Command {

        private final ObjectMapper commandMapper = new ObjectMapper();

        @Override
        public void execute(MultivaluedMap<String, String> queryParameters, PrintWriter entityWriter) throws Exception {
            Commands.outputFormattedJson(commandMapper, entityWriter, queryParameters, ImmutableMap.of("k1", "v1", "k2", "v2"));
        }
    }

    public static final class SuccessfulEmptyEntityCommand implements Command {

        @Override
        public void execute(MultivaluedMap<String, String> queryParameters, PrintWriter entityWriter) throws Exception {
            // noop
        }
    }

    public static final class ExceptionThrowingCommand implements Command {

        @Override
        public void execute(MultivaluedMap<String, String> queryParameters, PrintWriter entityWriter) throws Exception {
            throw new RuntimeException("FAILED");
        }
    }


    //
    // instance variables
    //

    private final ObjectMapper mapper = new ObjectMapper();
    {
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }

    @Rule
    public final HttpClientResource client = new HttpClientResource();

    //
    // applies to any command
    //

    @Test
    public void shouldReturn_HTTP_OK_WithNonEmptyEntityWhenCommandExecuted() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                admin.registerCommand("cmd", SuccessfulNonEmptyEntityCommand.class);
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/cmd");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
            assertThat(HttpUtils.readResponseEntityToString(response), equalTo("EXECUTED"));
        }  finally {
            service.shutdown();
        }
    }

    @Test
    public void shouldReturn_HTTP_OK_WithNonPrettyPrintedNonEmptyEntityWhenCommandExecuted() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                admin.registerCommand("cmd", SuccessfulJsonEntityCommand.class);
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/cmd");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));

            // get the string version of the json
            String remotelyPrinted = HttpUtils.readResponseEntityToString(response);

            // get the object version of it
            Object created = mapper.readValue(remotelyPrinted, Object.class);

            // do *not* pretty-print it locally
            String locallyPrinted = mapper.writeValueAsString(created);

            // check that they're identical
            assertThat(locallyPrinted, equalTo(remotelyPrinted));
        }  finally {
            service.shutdown();
        }
    }

    @Test
    public void shouldReturn_HTTP_OK_WithPrettyPrintedNonEmptyEntityWhenCommandExecuted() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                admin.registerCommand("cmd", SuccessfulJsonEntityCommand.class);
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/cmd?pretty");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));

            // get the string version of the json
            String remotelyPrettyPrinted = HttpUtils.readResponseEntityToString(response);

            // get the object version of it
            Object created = mapper.readValue(remotelyPrettyPrinted, Object.class);

            // pretty-print it locally
            String locallyPrettyPrinted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(created);

            // check that they're identical
            assertThat(locallyPrettyPrinted, equalTo(remotelyPrettyPrinted));
        }  finally {
            service.shutdown();
        }
    }

    @Test
    public void shouldReturn_HTTP_OK_WithEmptyEntityWhenCommandExecuted() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                admin.registerCommand("cmd", SuccessfulEmptyEntityCommand.class);
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/cmd");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
            assertThat(response.getEntity().getContentLength(), equalTo(0L));
        }  finally {
            service.shutdown();
        }
    }

    @Test
    public void shouldReturn_HTTP_SERVICE_UNAVAILABLE_WithCustomMessageAndCustomErrorCodeWhenFailingCommandExecuted() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                admin.registerCommand("cmd", ExceptionThrowingCommand.class);

                // set an exception mapper for runtime exceptions
                admin.addProvider(new BaseExceptionMapper<RuntimeException>(BaseExceptionMapper.ErrorResponseEntity.NO_STACK_IN_RESPONSE, BaseExceptionMapper.StackLogging.DISABLE_LOGGING) {
                    @Override
                    protected int getErrorCode(RuntimeException throwable) {
                        return 777;
                    }

                    @Override
                    protected String getErrorName(RuntimeException throwable) {
                        return "CUSTOM";
                    }

                    @Override
                    protected String getErrorText(RuntimeException throwable) {
                        return "BAD_COMMAND";
                    }

                    @Override
                    protected Response.Status getHttpResponseStatus(RuntimeException throwable) {
                        return Response.Status.SERVICE_UNAVAILABLE;
                    }
                });
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/cmd");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_SERVICE_UNAVAILABLE));

            // this is the result we expect
            ServiceError expected = new ServiceError();
            expected.setErrorCode(777);
            expected.setErrorName("CUSTOM");
            expected.setErrorType("RuntimeException"); // exception type
            expected.setErrorText("BAD_COMMAND");

            // convert it into json
            ServiceError returned = mapper.readValue(response.getEntity().getContent(), ServiceError.class);

            // check the result
            assertThat(returned, equalTo(expected));
        }  finally {
            service.shutdown();
        }
    }

    @Test
    public void shouldReturn_HTTP_INTERNAL_SERVER_ERROR_WithThrowableMessageAndGeneralErrorCodeWhenCommandThrowsDuringExecution() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                admin.registerCommand("cmd", ExceptionThrowingCommand.class);
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/cmd");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_INTERNAL_SERVER_ERROR));

            // this is the result we expect
            ServiceError expected = new ServiceError();
            expected.setErrorCode(BaselineError.GENERAL.code());
            expected.setErrorName(BaselineError.GENERAL.name());
            expected.setErrorType("RuntimeException"); // exception type
            expected.setErrorText("FAILED"); // exception message

            // convert it into json
            ServiceError returned = mapper.readValue(response.getEntity().getContent(), ServiceError.class);

            // check the result
            assertThat(returned, equalTo(expected));
        }  finally {
            service.shutdown();
        }
    }

    //
    // applies to the two bundled commands
    //

    @Test
    public void shouldReturn_HTTP_OK_WhenGarbageCollectionCommandExecuted() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                // don't register anything beyond the defaults
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/gc");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));
            String body = HttpUtils.readStreamToString(response.getEntity().getContent());
            assertThat(body.trim().equalsIgnoreCase("Running gc..."), equalTo(true));
        } finally {
            service.shutdown();
        }
    }

    @Test
    public void shouldReturn_HTTP_OK_WithJsonMetricValuesWhenMetricsCommandExecuted() throws Exception {
        Service<ServiceConfiguration> service = new Service<ServiceConfiguration>("test") {

            @Override
            public void init(ServiceConfiguration configuration, RootEnvironment root, AdminEnvironment admin, ServiceEnvironment service) throws Exception {
                // don't register anything beyond the defaults
            }
        };

        try {
            // setup the server
            service.runWithConfiguration(ServiceConfiguration.TEST_CONFIGURATION);

            // attempt to execute the command
            HttpPost post = new HttpPost(ServiceConfiguration.ADMIN_URL + "/commands/metrics");
            Future<HttpResponse> future = client.getClient().execute(post, null);
            HttpResponse response = future.get();

            // verify the result
            assertThat(response.getStatusLine().getStatusCode(), equalTo(HttpStatus.SC_OK));

            // the returned body should be valid json
            String body = HttpUtils.readStreamToString(response.getEntity().getContent());
            JsonNode metrics = mapper.readTree(body);
            assertThat(metrics, notNullValue());

            // check that at least the jvm metrics exist
            assertThat(metrics.findValue("jvm"),notNullValue());

            // check that the incoming metrics *are not* pretty-printed
            String pretty = mapper.writeValueAsString(mapper.treeToValue(metrics, Object.class));
            assertThat(pretty.equalsIgnoreCase(body), equalTo(true));
        } finally {
            service.shutdown();
        }
    }
}