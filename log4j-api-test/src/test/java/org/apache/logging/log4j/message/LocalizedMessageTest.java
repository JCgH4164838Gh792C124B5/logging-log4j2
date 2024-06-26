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
package org.apache.logging.log4j.message;

import java.util.Locale;

import org.apache.logging.log4j.test.junit.Mutable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests LocalizedMessage.
 */
@ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
public class LocalizedMessageTest {

    @Test
    public void testMessageFormat() {
        final LocalizedMessage msg = new LocalizedMessage("MF", new Locale("en", "US"), "msg1", new Object[] { "1", "Test" });
        assertEquals("This is test number 1 with string argument Test.", msg.getFormattedMessage());
    }

    @Test
    public void testStringFormat() {
        final LocalizedMessage msg = new LocalizedMessage("SF", new Locale("en", "US"), "msg1", new Object[] { "1", "Test" });
        assertEquals("This is test number 1 with string argument Test.", msg.getFormattedMessage());
    }

    @Test
    public void testUnsafeWithMutableParams() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final LocalizedMessage msg = new LocalizedMessage(testMsg, param);

        // modify parameter before calling msg.getFormattedMessage
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Test message XYZ", actual, "Expected most recent param value");
    }

    @Test
    public void testSafeAfterGetFormattedMessageIsCalled() { // LOG4J2-763
        final String testMsg = "Test message %s";
        final Mutable param = new Mutable().set("abc");
        final LocalizedMessage msg = new LocalizedMessage(testMsg, param);

        // modify parameter after calling msg.getFormattedMessage
        msg.getFormattedMessage();
        param.set("XYZ");
        final String actual = msg.getFormattedMessage();
        assertEquals("Test message abc", actual, "Should use initial param value");
    }

    @Test
    @ResourceLock(Resources.LOCALE)
    public void testMessageUsingBaseName() { // LOG4J2-2850
        final Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            final String testMsg = "hello_world";
            final LocalizedMessage msg = new LocalizedMessage("MF", testMsg, null);
            assertEquals("Hello world.", msg.getFormattedMessage());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
