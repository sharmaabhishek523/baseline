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

import com.google.common.collect.Lists;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@NotThreadSafe
public final class LifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleManager.class);

    private final List<Managed> managedServices = Lists.newArrayListWithCapacity(32);
    private final ScheduledThreadPoolExecutor scheduledExecutorService = new ScheduledThreadPoolExecutor(Constants.DEFAULT_SCHEDULED_THREAD_POOL_EXECUTOR_SIZE, Threads.newNamedThreadFactory("sched-thd-%d"));
    private final Timer timer = new HashedWheelTimer();

    LifecycleManager() {
        addManaged(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("start timer");
            }

            @Override
            public void stop() {
                LOGGER.info("stop timer");
                timer.stop();
            }
        });

        addManaged(new Managed() {
            @Override
            public void start() throws Exception {
                LOGGER.info("start scheduled executor");
            }

            @Override
            public void stop() {
                LOGGER.info("stop scheduled executor");
                scheduledExecutorService.shutdownNow();
            }
        });
    }

    public Timer getTimer() {
        return timer;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public void addManaged(Managed managed) {
        managedServices.add(managed) ;
    }

    void start() throws Exception {
        for (Managed managed : managedServices) {
            try {
                managed.start();
            } catch (Exception e) {
                LOGGER.warn("fail start managed:{}", managed.getClass().getSimpleName(), e);
                throw e;
            }
        }
    }

    void stop() {
        for (Managed managed : Lists.reverse(managedServices)) {
            try {
                managed.stop();
            } catch (Exception e) {
                LOGGER.warn("fail stop managed:{}", managed.getClass().getSimpleName(), e);
            }
        }
    }
}
