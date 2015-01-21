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

package com.aerofs.baseline.http;

/**
 * Contains headers and header values used by AeroFS backend services.
 */
public abstract class Headers {

    /**
     * Header that encodes the request id associated with the incoming HTTP request.
     * This request id is <strong>per service</strong>.
     */
    public static final String BASELINE_REQUEST_HEADER = "Baseline-Request-Id";

    /**
     * Header that encodes the error id associated with a failed HTTP request.
     * This error id can be used to correlate a failed request with log messages in the service.
     */
    public static final String BASELINE_FAILURE_HEADER = "Baseline-Failure-Id";

    private Headers() {
        // to prevent instantiation by subclasses
    }
}
