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

import com.aerofs.baseline.errors.BaseExceptionMapper;
import com.aerofs.baseline.errors.BaselineError;

import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

@ThreadSafe
@Singleton
public final class IllegalArgumentExceptionMapper extends BaseExceptionMapper<IllegalArgumentException> {

    public IllegalArgumentExceptionMapper() {
        super(ErrorResponseEntity.NO_STACK_IN_RESPONSE, StackLogging.ENABLE_LOGGING);
    }

    @Override
    protected int getErrorCode(IllegalArgumentException throwable) {
        return BaselineError.INVALID_INPUT_PARAMETERS.code();
    }

    @Override
    protected String getErrorName(IllegalArgumentException throwable) {
        return BaselineError.INVALID_INPUT_PARAMETERS.name();
    }

    @Override
    protected String getErrorText(IllegalArgumentException throwable) {
        return throwable.getMessage();
    }

    protected Response.Status getHttpResponseStatus(IllegalArgumentException throwable) {
        return Response.Status.BAD_REQUEST;
    }
}
