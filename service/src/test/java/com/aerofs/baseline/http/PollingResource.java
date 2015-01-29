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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import java.util.concurrent.TimeUnit;

@Singleton
@Path("/" + Resources.POLLING_RESOURCE)
public final class PollingResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollingResource.class);

    @GET
    public void poll(@Suspended final AsyncResponse asyncResponse) {
        LOGGER.debug("polling");

        // set timeout multiple times to ensure that we handle this properly
        asyncResponse.setTimeout(4, TimeUnit.SECONDS);
        asyncResponse.setTimeout(6, TimeUnit.SECONDS);
        asyncResponse.setTimeout(12, TimeUnit.SECONDS);

        new Thread(() -> {
            try {
                Thread.sleep(8000);
                asyncResponse.resume("success");
            } catch (InterruptedException e) {
                LOGGER.warn("interrupted during polling sleep");
            }
        }).start();
    }
}
