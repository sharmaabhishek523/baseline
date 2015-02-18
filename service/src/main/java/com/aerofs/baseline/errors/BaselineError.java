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

package com.aerofs.baseline.errors;

import javax.annotation.concurrent.Immutable;

@Immutable
public enum BaselineError {

    GENERAL(111),

    UNPARSEABLE_INPUT(112),

    INVALID_INPUT_PARAMETERS(113),

    INVALID_ADMIN_COMMAND(114);

    private final int code;

    BaselineError(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
