/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.time.internal;

import java.util.concurrent.locks.LockSupport;

import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.util.Lazy;

/**
 * This Clock implementation is similar to CachedClock. It is slightly faster at
 * the cost of some accuracy.
 */
public final class CoarseCachedClock implements Clock {
    private static final Lazy<CoarseCachedClock> INSTANCE = Lazy.lazy(CoarseCachedClock::new);
    // ignore IDE complaints; volatile long is fine
    private volatile long millis = System.currentTimeMillis();

    private CoarseCachedClock() {
        Thread updater = new Log4jThread("CoarseCachedClock Updater Thread") {
            @Override
            public void run() {
                while (true) {
                    millis = System.currentTimeMillis();

                    // avoid explicit dependency on sun.misc.Util
                    LockSupport.parkNanos(1000 * 1000);
                }
            }
        };
        updater.setDaemon(true);
        updater.start();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the singleton instance
     */
    public static CoarseCachedClock instance() {
        // LOG4J2-819: use lazy initialization of threads
        return INSTANCE.value();
    }

    /**
     * Returns the value of a private long field that is updated by a background
     * thread once every millisecond. Because timers on most platforms do not
     * have millisecond granularity, the returned value may "jump" every 10 or
     * 16 milliseconds.
     * @return the cached time
     */
    @Override
    public long currentTimeMillis() {
        return millis;
    }
}
