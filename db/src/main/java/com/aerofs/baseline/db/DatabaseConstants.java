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

import java.sql.Connection;
import java.util.concurrent.TimeUnit;

public abstract class DatabaseConstants {

    // database
    public static final int MIN_IDLE_CONNECTIONS = 1;
    public static final int DEFAULT_TRANSACTION_ISOLATION_LEVEL = Connection.TRANSACTION_SERIALIZABLE;
    public static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);
    public static final long DEFAULT_QUERY_TIMEOUT = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
    public static final int MAX_IDLE_CONNECTIONS = 10;
    public static final int MAX_TOTAL_CONNECTIONS = 10;

    private DatabaseConstants() {
        // to prevent instantiation by subclasses
    }
}
