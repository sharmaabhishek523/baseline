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

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Providers;

@SuppressWarnings({"rawtypes", "unchecked", "unused"})
@ThreadSafe
@Singleton
public final class DBIExceptionMapper implements ExceptionMapper<DBIException> {

    private final Providers providers;

    public DBIExceptionMapper(@Context Providers providers) {
        this.providers = providers;
    }

    @Override
    public Response toResponse(DBIException throwable) {
        Throwable cause = Databases.findRootCause(throwable);
        ExceptionMapper actualMapper = providers.getExceptionMapper(cause.getClass());
        return actualMapper.toResponse(cause);
    }
}
