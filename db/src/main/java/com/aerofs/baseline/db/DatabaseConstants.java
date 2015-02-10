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

package com.aerofs.baseline.db;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.TimeUnit;

@Immutable
public abstract class DatabaseConstants {

    public static final TransactionIsolation DEFAULT_TRANSACTION_ISOLATION = TransactionIsolation.SERIALIZABLE;

    public static final boolean DEFAULT_AUTO_COMMIT_ENABLED = false;

    public static final boolean DEFAULT_CHECK_CONNECTION_ON_RETURN = false;

    public static final boolean DEFAULT_CHECK_CONNECTION_ON_BORROW = false;

    public static final int DEFAULT_MIN_TOTAL_CONNECTIONS = 10;

    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 10;

    public static final long DEFAULT_ACQUIRE_CONNECTION_TIMEOUT = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);

    public static final long DEFAULT_QUERY_TIMEOUT = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);

    public static final int DEFAULT_MAX_IDLE_CONNECTIONS = 10;

    public static final long DEFAULT_IDLE_CONNECTION_TIMEOUT = TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS);

    public static final boolean DEFAULT_CHECK_CONNECTION_WHEN_IDLE = true;

    public static final long DEFAULT_IDLE_CONNECTION_HEALTH_CHECK_TIMEOUT = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);

    public static final String DEFAULT_VALIDATION_QUERY = "/* DB HEALTH CHECK */ SELECT 1";

    public static final long DEFAULT_VALIDATION_QUERY_TIMEOUT = DEFAULT_QUERY_TIMEOUT;

    private DatabaseConstants() {
        // to prevent instantiation by subclasses
    }
}
