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

import com.google.common.base.Objects;
import org.hibernate.validator.constraints.NotBlank;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
public final class DatabaseConfiguration {

    @NotBlank
    private String driverClass;

    @NotBlank
    private String url;

    @NotNull
    private String username = "";

    @NotNull
    private String password = "";

    @Min(0)
    private int minIdleConnections = DatabaseConstants.MIN_IDLE_CONNECTIONS;

    @Min(0)
    private int maxIdleConnections = DatabaseConstants.MAX_IDLE_CONNECTIONS;

    @Min(1)
    private int maxTotalConnections = DatabaseConstants.MAX_TOTAL_CONNECTIONS;

    @Min(1)
    private int defaultQueryTimeout = (int) DatabaseConstants.DEFAULT_QUERY_TIMEOUT;

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMinIdleConnections() {
        return minIdleConnections;
    }

    public void setMinIdleConnections(int minIdleConnections) {
        this.minIdleConnections = minIdleConnections;
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getDefaultQueryTimeout() {
        return defaultQueryTimeout;
    }

    public void setDefaultQueryTimeout(int defaultQueryTimeout) {
        this.defaultQueryTimeout = defaultQueryTimeout;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DatabaseConfiguration other = (DatabaseConfiguration) o;
        return Objects.equal(driverClass, other.driverClass)
                && Objects.equal(url, other.url)
                && Objects.equal(username, other.username)
                && Objects.equal(password, other.password)
                && minIdleConnections == other.minIdleConnections
                && maxIdleConnections == other.maxIdleConnections
                && maxTotalConnections == other.maxTotalConnections
                && defaultQueryTimeout == other.defaultQueryTimeout;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(driverClass, url, username, password, minIdleConnections, maxIdleConnections, maxTotalConnections, defaultQueryTimeout);
    }

    @Override
    public String toString() {
        return Objects
                .toStringHelper(this)
                .add("driverClass", driverClass)
                .add("url", url)
                .add("username", username)
                .add("password", password)
                .add("minIdleConnections", minIdleConnections)
                .add("maxIdleConnections", maxIdleConnections)
                .add("maxTotalConnections", maxTotalConnections)
                .add("defaultQueryTimeout", defaultQueryTimeout)
                .toString();
    }
}
