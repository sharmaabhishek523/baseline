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

import com.aerofs.baseline.RootEnvironment;
import com.aerofs.baseline.metrics.MetricRegistries;
import com.codahale.metrics.Gauge;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import javax.sql.DataSource;

/**
 * Creates a JDBC connection pool.
 */
public abstract class DataSources {

    public static DataSource newDataSource(DatabaseConfiguration database) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setDriverClassName(database.getDriverClass());
        dataSource.setUsername(database.getUsername());
        if (!database.getPassword().isEmpty()) {
            dataSource.setPassword(database.getPassword());
        }
        dataSource.setUrl(database.getUrl());
        dataSource.setMinIdle(database.getMinIdleConnections());
        dataSource.setMaxIdle(database.getMaxIdleConnections());
        dataSource.setMaxTotal(database.getMaxTotalConnections());
        dataSource.setDefaultQueryTimeout(database.getDefaultQueryTimeout());
        dataSource.setTimeBetweenEvictionRunsMillis(DatabaseConstants.DEFAULT_TIME_BETWEEN_EVICTION_RUNS);
        dataSource.setDefaultTransactionIsolation(DatabaseConstants.DEFAULT_TRANSACTION_ISOLATION_LEVEL);
        dataSource.setAccessToUnderlyingConnectionAllowed(false);
        dataSource.setDefaultAutoCommit(false);

        return dataSource;
    }

    private static void registerGauges(final BasicDataSource dataSource) {
        MetricRegistries.getRegistry().register(MetricRegistries.name("db", dataSource.getUrl(), "used"), (Gauge<Integer>) dataSource::getNumActive);
        MetricRegistries.getRegistry().register(MetricRegistries.name("db", dataSource.getUrl(), "idle"), (Gauge<Integer>) dataSource::getNumIdle);
    }

    public static ManagedDataSource newManagedDataSource(RootEnvironment root, DatabaseConfiguration database) {
        BasicDataSource dataSource = (BasicDataSource) newDataSource(database);
        registerGauges(dataSource);

        ManagedDataSource managed = new ManagedDataSource(dataSource);
        root.addManaged(managed);

        return managed;
    }

    private DataSources() {
        // to prevent instantiation by subclasses
    }
}
