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

import org.skife.jdbi.v2.exceptions.DBIException;

public abstract class DBIExceptions {

    public static Throwable findRootCause(DBIException exception) {
        Throwable underlying = exception;

        while(underlying != null && underlying instanceof DBIException) {
            underlying = underlying.getCause();
        }

        // if we can't find a non-DBIException at the base,
        // just return the topmost exception
        if (underlying == null) {
            underlying = exception;
        }

        return underlying;
    }

    private DBIExceptions() {
        // to prevent instantiation by subclasses
    }
}
