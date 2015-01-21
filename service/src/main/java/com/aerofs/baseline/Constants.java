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

import ch.qos.logback.classic.Level;

import javax.annotation.concurrent.Immutable;

@Immutable
public abstract class Constants {

    // exit codes
    public static final int INVALID_ARGUMENTS_EXIT_CODE = 10;
    public static final int INVALID_CONFIGURATION_EXIT_CODE = 11;
    public static final int FAIL_SERVICE_START_EXIT_CODE = 12;

    // something dun fucked up exit code
    public static final int UNCAUGHT_EXCEPTION_ERROR_CODE = 999;

    // baseline initializes two environments with the following identifiers
    public static final String ADMIN_IDENTIFIER = "adm";
    public static final String SERVICE_IDENTIFIER = "svc";

    // threading
    public static final int DEFAULT_SCHEDULED_THREAD_POOL_EXECUTOR_SIZE = 2;

    // metric names
    public static final String JVM_BUFFERS = "jvm.buffers";
    public static final String JVM_GC = "jvm.gc";
    public static final String JVM_MEMORY = "jvm.memory";
    public static final String JVM_THREADS = "jvm.threads";

    // logging
    public static final String DEFAULT_LOG_LEVEL = Level.INFO.levelStr;
    public static final String DEFAULT_LOGFILE_NAME = "server.log";

    // admin parameters
    public static final String JSON_COMMAND_RESPONSE_ENTITY_PRETTY_PRINTING_QUERY_PARAMETER = "pretty";
    public static final int DEFAULT_COMMAND_RESPONSE_ENTITY_LENGTH = 1024;

    // injectable
    public static final String APP_NAME_KEY = "APP_NAME_KEY";

    private Constants() {
        // to prevent instantiation by subclasses
    }
}
