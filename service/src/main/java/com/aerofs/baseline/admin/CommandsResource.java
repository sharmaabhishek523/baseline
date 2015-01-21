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

import com.aerofs.baseline.Constants;
import com.aerofs.baseline.RootEnvironment;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Root resource class for a set of admin commands
 * registered with baseline service.
 * <p>
 * A baseline service may implement zero or more admin commands
 * that may introspect the state of the service or modify
 * service operation. The requested command is specified as the
 * final path component in the url, and may be passed options
 * via query parameters.
 * <p>
 * If a command executes successfully an HTTP response with
 * status code {@code 200} and an optional entity is returned.
 * This optional entity contains any output the command
 * wants to return to the requester. If the command terminates
 * early because of an exception an HTTP response with an error
 * status code and an error entity is returned. The exact
 * response content is determined by the {@link javax.ws.rs.ext.ExceptionMapper}
 * that matches and handles the exception.
 * <p>
 * If the command requested is <strong>not found</strong> an
 * HTTP response with status code {@code 404} and an empty entity
 * is returned.
 * <p>
 * This resource is accessed via:
 * <pre>
 *     curl http://service_url:service_admin_port/commands/command_name[?query_parameter...]
 * </pre>
 */
@Path("/commands")
@Singleton
@ThreadSafe
public final class CommandsResource {

    private final RootEnvironment root;
    private final ImmutableMap<String, Class<Command>> commands;

    @SuppressWarnings({"rawtypes", "unchecked", "unused"})
    @Inject
    private CommandsResource(RootEnvironment root, @Named("commands") ImmutableMap commands) {
        this.root = root;
        this.commands = commands;
    }

    @Path("/{command}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public InputStream runTask(@PathParam("command") String name, @Context UriInfo uriInfo) throws Exception {
        Class<Command> commandClass = commands.get(name);

        if (commandClass == null) {
            throw new InvalidCommandException(name);
        }

        // safe, since close() on this is a noop
        ByteArrayOutputStream underlying = new ByteArrayOutputStream(Constants.DEFAULT_COMMAND_RESPONSE_ENTITY_LENGTH);

        // if the command bails or the write fails, we throw
        // and let baseline take care of returning the appropriate
        // error response
        try (PrintWriter writer = new PrintWriter(new PrintStream(underlying, true, Charsets.UTF_8.name()))) {
            // don't allow the commands to change the parameters
            ImmutableMultivaluedMap<String, String> queryParameters = new ImmutableMultivaluedMap<>(uriInfo.getQueryParameters());

            // hmm...apparently these instances have to be cached manually?
            // see SubResourceLocatorRouter.java@2.14#L146
            // see JerseyResourceContext.java@2.14#L153
            // find and execute the command
            Command command = root.getOrCreateInstance(commandClass);
            command.execute(queryParameters, writer);

            // flush the writer explicitly to force it
            // to write through its buffered data
            writer.flush();

            // return the entity content
            return new ByteArrayInputStream(underlying.toByteArray());
        }
    }
}
