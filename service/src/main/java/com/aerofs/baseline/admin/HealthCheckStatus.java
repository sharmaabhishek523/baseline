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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Result of a {@link HealthCheck} execution.
 */
@Immutable
public final class HealthCheckStatus {

    /**
     * Status of a {@link HealthCheck} execution.
     */
    public enum Status {
        /** The health check executed successfully. */
        SUCCESS,

        /** The health check failed. */
        FAILURE,
    }

    private static final HealthCheckStatus SUCCESS = new HealthCheckStatus(Status.SUCCESS);
    private static final HealthCheckStatus FAILURE = new HealthCheckStatus(Status.FAILURE);

    /**
     * Factory method to return an instance of {@link HealthCheckStatus}
     * with {@code status} of {@code SUCCESS} and no {@code message}.
     *
     * @return instance with {@code status} of {@code SUCCESS} and no {@code message}
     */
    public static HealthCheckStatus success() {
        return SUCCESS;
    }

    /**
     * Factory method to return an instance of {@link HealthCheckStatus}
     * with {@code status} of {@code FAILURE} and no {@code message}.
     *
     * @return {@code HealthCheckStatus} instance with {@code status} of
     * {@code FAILURE} and no {@code message}
     */
    public static HealthCheckStatus failure() {
        return FAILURE;
    }

    /**
     * Factory method to return an instance of {@link HealthCheckStatus}
     * with {@code status} of {@code FAILURE} and the user-specified
     * {@code message}.
     *
     * @param message explanatory message describing the status failure
     *
     * @return {@code HealthCheckStatus} instance with {@code status} of {@code FAILURE}
     * and the user-specified {@code message}
     */
    public static HealthCheckStatus failure(String message) {
        Preconditions.checkNotNull(message, "cannot use null failure message");
        return new HealthCheckStatus(Status.FAILURE, message);
    }

    //
    // actual health check status class
    // you can only create instances via the statics above
    //

    private final Status status;

    @Nullable
    private final String message;

    private HealthCheckStatus(Status status) {
        this(status, null);
    }

    // NOTE: we can have only *one* constructor annotated with JsonCreator, so we choose the most general one
    // http://stackoverflow.com/questions/15931082/how-to-deserialize-a-class-with-overloaded-constructors-using-jsoncreator
    @JsonCreator
    private HealthCheckStatus(@JsonProperty("status") Status status, @JsonProperty("message") @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * @return the health check {@code Status} indicating whether the health check succeeded or failed
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return optional message explaining {@link HealthCheckStatus#getStatus}
     */
    public final @Nullable String getMessage() {
        return message;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HealthCheckStatus other = (HealthCheckStatus) o;
        return Objects.equal(status, other.status) && Objects.equal(message, other.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(status, message);
    }

    @Override
    public String toString() {
        return Objects
                .toStringHelper(this)
                .add("status", status)
                .add("message", message)
                .toString();
    }
}
