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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SetSystemProperty(key = Log4jProperties.CONFIG_RELIABILITY_STRATEGY, value = "org.apache.logging.log4j.core.config.MockReliabilityStrategy")
class ReliabilityStrategyTest {

    @Test
    @LoggerContextSource("ReliabilityStrategyTest.xml")
    void beforeStopAppendersCalledBeforeAsyncAppendersStopped(@Named final AsyncAppender async, final Configuration config) {
        assertTrue(async.isStarted());
        final MockReliabilityStrategy reliabilityStrategy =
                (MockReliabilityStrategy) config.getRootLogger().getReliabilityStrategy();
        config.stop();
        assertTrue(async.isStopped());
        reliabilityStrategy.rethrowAssertionErrors();
    }
}
