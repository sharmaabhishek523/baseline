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
import com.codahale.metrics.jdbi.InstrumentedTimingCollector;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.exceptions.DBIException;

import javax.annotation.concurrent.ThreadSafe;
import javax.sql.DataSource;

@SuppressWarnings("unused")
@ThreadSafe
public abstract class Databases {

    public static DBI newDBI(DataSource dataSource) {
        DBI dbi = new DBI(dataSource);

        dbi.setSQLLog(new LogbackSQLLog());
        dbi.setTimingCollector(new InstrumentedTimingCollector(MetricRegistries.getRegistry()));

        return dbi;
    }

    public static ManagedDataSource newManagedDataSource(RootEnvironment root, DatabaseConfiguration database) {
        BasicDataSource dataSource = (BasicDataSource) newDataSource(database);
        registerGauges(dataSource);

        ManagedDataSource managed = new ManagedDataSource(dataSource);
        root.addManaged(managed);

        return managed;
    }

    public static DataSource newDataSource(DatabaseConfiguration database) {
        BasicDataSource dataSource = new BasicDataSource();

        // attempt to load the driver class
        dataSource.setDriverClassName(database.getDriverClass());

        // username and password
        if (!(database.getUsername() == null || database.getUsername().isEmpty())) {
            dataSource.setUsername(database.getUsername());
        }
        if (!(database.getPassword() == null || database.getPassword().isEmpty())) {
            dataSource.setPassword(database.getPassword());
        }

        // other properties
        dataSource.setUrl(database.getUrl());
        dataSource.setDefaultTransactionIsolation(database.getDefaultTransactionIsolation().getLevel());
        dataSource.setDefaultAutoCommit(database.isAutoCommit());
        dataSource.setTestOnBorrow(database.isCheckConnectionOnBorrow());
        dataSource.setTestOnReturn(database.isCheckConnectionOnReturn());
        dataSource.setMinIdle(database.getMinTotalConnections());
        dataSource.setMaxTotal(database.getMaxTotalConnections());
        dataSource.setMaxIdle(database.getMaxIdleConnections());
        dataSource.setMaxWaitMillis(database.getAcquireConnectionTimeout());
        dataSource.setDefaultQueryTimeout(database.getQueryTimeout() <= 0 ? null : database.getQueryTimeout());
        dataSource.setTestWhileIdle(database.isCheckConnectionWhenIdle());
        dataSource.setTimeBetweenEvictionRunsMillis(database.getCheckIdleConnectionHealthAfter());
        dataSource.setMinEvictableIdleTimeMillis(database.getCloseIdleConnectionAfter());
        dataSource.setValidationQuery(database.getValidationQuery());
        dataSource.setValidationQueryTimeout((int) database.getValidationQueryTimeout());
        dataSource.setAccessToUnderlyingConnectionAllowed(false);
        dataSource.setDefaultAutoCommit(false);

        return dataSource;
    }

    private static void registerGauges(final BasicDataSource dataSource) {
        MetricRegistries.getRegistry().register(MetricRegistries.name("db", dataSource.getUrl(), "used"), (Gauge<Integer>) dataSource::getNumActive);
        MetricRegistries.getRegistry().register(MetricRegistries.name("db", dataSource.getUrl(), "idle"), (Gauge<Integer>) dataSource::getNumIdle);
    }

    public static Throwable findRootCause(DBIException exception) {
        Throwable underlying = exception;

        while(underlying != null && underlying instanceof DBIException) {
            underlying = underlying.getCause();
        }

        // if it's DBIExceptions all the way down just return the topmost exception
        if (underlying == null) {
            underlying = exception;
        }

        return underlying;
    }

    private Databases() {
        // to prevent instantiation by subclasses
    }
}
