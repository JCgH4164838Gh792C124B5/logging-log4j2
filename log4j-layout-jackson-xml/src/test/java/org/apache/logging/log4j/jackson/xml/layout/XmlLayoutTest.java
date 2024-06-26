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
package org.apache.logging.log4j.jackson.xml.layout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.layout.LogEventFixtures;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.jackson.AbstractJacksonLayout;
import org.apache.logging.log4j.jackson.XmlConstants;
import org.apache.logging.log4j.jackson.xml.AbstractLogEventXmlMixIn;
import org.apache.logging.log4j.jackson.xml.Log4jXmlObjectMapper;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.apache.logging.log4j.util.Lazy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link XmlLayout}.
 */
@UsingAnyThreadContext
public class XmlLayoutTest {
    private static final int NOT_FOUND = -1;

    @AfterAll
    public static void cleanupClass() {
        LoggerContext.getContext().stop();
    }

    @BeforeAll
    public static void setupClass() {
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.getInjector().registerBinding(ConfigurationFactory.KEY, Lazy.lazy(BasicConfigurationFactory::new));
        ctx.reconfigure();
    }

    LoggerContext ctx = LoggerContext.getContext();

    Logger rootLogger = this.ctx.getRootLogger();

    private void checkAttribute(final String name, final String value, final boolean compact, final String str) {
        assertTrue(str.contains(name + "=\"" + value + "\""), str);
    }

    private void checkAttributeName(final String name, final boolean compact, final String str) {
        assertTrue(str.contains(name + "=\""), str);
    }

    private void checkContains(final String expected, final List<String> list) {
        for (final String string : list) {
            final String trimedLine = string.trim();
            if (trimedLine.contains(expected)) {
                return;
            }
        }
        fail("Cannot find " + expected + " in " + list);
    }

    private void checkContextMapElement(final String key, final String value, final boolean compact, final String str) {
        // <item key="MDC.A" value="A_Value"/>
        assertTrue(str.contains(String.format("<item key=\"%s\" value=\"%s\"/>", key, value)), str);
    }

    private void checkContextStackElement(final String value, final boolean compact, final String str) {
        // <ContextStackItem>stack_msg1</ContextStackItem>
        assertTrue(str.contains(String.format("<ContextStackItem>%s</ContextStackItem>", value)), str);
    }

    private void checkElementName(final String name, final boolean compact, final String str,
            final boolean withAttributes, final boolean withChildren) {
        // simple checks, don't try to be too smart here, we're just looking for the names and basic shape.
        // start
        final String startStr = withAttributes ? "<" + name + " " : "<" + name + ">";
        final int startPos = str.indexOf(startStr);
        assertTrue(startPos >= 0, String.format("Missing text '%s' in: %s", startStr, str));
        // end
        final String endStr = withChildren ? "</" + name + ">" : "/>";
        final int endPos = str.indexOf(endStr, startPos + startStr.length());
        assertTrue(endPos >= 0, str);
    }

    private void checkElementNameAbsent(final String name, final boolean compact, final String str) {
        assertFalse(str.contains("<" + name));
    }

    private void checkJsonPropertyOrder(final boolean includeContextStack, final boolean includeContextMap,
            final boolean includeStacktrace, final String str) {
        final JsonPropertyOrder annotation = AbstractLogEventXmlMixIn.class.getAnnotation(JsonPropertyOrder.class);
        Assertions.assertNotNull(annotation);
        int previousIndex = 0;
        String previousName = null;
        for (final String name : annotation.value()) {
            final int currentIndex = str.indexOf(name);
            if (!includeContextStack && XmlConstants.ELT_CONTEXT_STACK.equals(name)) {
                assertEquals(NOT_FOUND, currentIndex, String.format("Unexpected element '%s' in: %s", name, str));
                break;
            }
            if (!includeContextMap && XmlConstants.ELT_CONTEXT_MAP.equals(name)) {
                assertEquals(NOT_FOUND, currentIndex, String.format("Unexpected element '%s' in: %s", name, str));
                break;
            }
            if (!includeStacktrace && XmlConstants.ELT_EXTENDED_STACK_TRACE.equals(name)) {
                assertEquals(NOT_FOUND, currentIndex, String.format("Unexpected element '%s' in: %s", name, str));
                break;
            }
            if (!includeStacktrace && XmlConstants.ELT_EXTENDED_STACK_TRACE_ITEM.equals(name)) {
                assertEquals(NOT_FOUND, currentIndex, String.format("Unexpected element '%s' in: %s", name, str));
                break;
            }
            // TODO
            // Bug: The method
            // com.fasterxml.jackson.databind.introspect.POJOPropertiesCollector._sortProperties(Map<String,
            // POJOPropertyBuilder>) messes up the order defined in AbstractXmlLogEventMixIn's JsonPropertyOrder
            // annotations.
            // Assert.assertTrue(String.format("name='%s', previousIndex=%,d, previousName='%s', currentIndex=%,d: %s",
            // name, previousIndex, previousName, currentIndex, str), previousIndex < currentIndex);
            previousIndex = currentIndex;
            previousName = name;
        }
    }

    private String prepareXMLForStacktraceTests(final boolean stacktraceAsString) {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        // @formatter:off
        final AbstractJacksonLayout layout = XmlLayout.newBuilder()
                .setCompact(true)
                .setIncludeStacktrace(true)
                .setStacktraceAsString(stacktraceAsString)
                .build();
        // @formatter:off
        return layout.toSerializable(expected);
    }

    @Test
    public void testAdditionalFields() throws Exception {
        final AbstractJacksonLayout layout = XmlLayout.newBuilder().setLocationInfo(false).setProperties(false)
                .setIncludeStacktrace(false)
                .setAdditionalFields(new KeyValuePair[] { new KeyValuePair("KEY1", "VALUE1"),
                        new KeyValuePair("KEY2", "${java:runtime}"), })
                .setCharset(StandardCharsets.UTF_8).setConfiguration(ctx.getConfiguration()).build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertTrue(str.contains("<KEY1>VALUE1</KEY1>"), str);
        assertTrue(str.contains("<KEY2>" + new JavaLookup().getRuntime() + "</KEY2>"), str);
    }

    @Test
    public void testMutableLogEvent() throws Exception {
        final AbstractJacksonLayout layout = XmlLayout.newBuilder().setLocationInfo(false).setProperties(false)
                .setIncludeStacktrace(false)
                .setAdditionalFields(new KeyValuePair[] { new KeyValuePair("KEY1", "VALUE1"),
                        new KeyValuePair("KEY2", "${java:runtime}"), })
                .setCharset(StandardCharsets.UTF_8).setConfiguration(ctx.getConfiguration()).build();
        Log4jLogEvent logEvent = LogEventFixtures.createLogEvent();
        final MutableLogEvent mutableEvent = new MutableLogEvent();
        mutableEvent.initFrom(logEvent);
        final String strLogEvent = layout.toSerializable(logEvent);
        final String strMutableEvent = layout.toSerializable(mutableEvent);
        assertEquals(strLogEvent, strMutableEvent, strMutableEvent);
    }
    /**
     * @param includeLocationInfo
     *            TODO
     * @param compact
     * @param includeContextMap
     *            TODO
     * @param includeContextStack
     *            TODO
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonMappingException
     */
    private void testAllFeatures(final boolean includeLocationInfo, final boolean compact,
            final boolean includeContextMap, final boolean includeContextStack, final boolean includeStacktrace)
            throws IOException, JsonParseException, JsonMappingException {
        final Log4jLogEvent expected = LogEventFixtures.createLogEvent();
        // @formatter:off
        final XmlLayout layout = XmlLayout.newBuilder()
                .setLocationInfo(includeLocationInfo)
                .setProperties(includeContextMap)
                .setComplete(false)
                .setCompact(compact)
                .setIncludeStacktrace(includeStacktrace)
                .setCharset(StandardCharsets.UTF_8)
                .build();
        final String str = layout.toSerializable(expected);
        // @formatter:on
        // System.out.println(str);
        assertEquals(!compact, str.contains("\n"), str);
        assertEquals(includeLocationInfo, str.contains("Source"), str);
        assertEquals(includeContextMap, str.contains("ContextMap"), str);
        final Log4jLogEvent actual = new Log4jXmlObjectMapper().readValue(str, Log4jLogEvent.class);
        LogEventFixtures.assertEqualLogEvents(expected, actual, includeLocationInfo, includeContextMap,
                includeStacktrace);
        if (includeContextMap) {
            this.checkContextMapElement("MDC.A", "A_Value", compact, str);
            this.checkContextMapElement("MDC.B", "B_Value", compact, str);
        }
        if (includeContextStack) {
            this.checkContextStackElement("stack_msg1", compact, str);
            this.checkContextStackElement("stack_msg2", compact, str);
        }

        //
        assertNull(actual.getThrown());
        // check some attrs
        assertTrue(str.contains("loggerFqcn=\"f.q.c.n\""), str);
        assertTrue(str.contains("loggerName=\"a.B\""), str);
        // make sure short names are used
        assertTrue(str.contains("<Event "), str);
        if (includeStacktrace) {
            assertTrue(str.contains("class="), "Missing \"class=\" in: " + str);
            assertTrue(str.contains("method="), "Missing \"method=\" in: " + str);
            assertTrue(str.contains("file="), "Missing \"file=\" in: " + str);
            assertTrue(str.contains("line="), "Missing \"line=\" in: " + str);
        }
        //
        // make sure the names we want are used
        // this.checkAttributeName("timeMillis", compact, str);
        this.checkElementName("Instant", compact, str, true, false);
        this.checkAttributeName("epochSecond", compact, str);
        this.checkAttributeName("nanoOfSecond", compact, str);
        this.checkAttributeName("thread", compact, str); // and not threadName
        this.checkAttributeName("level", compact, str);
        this.checkAttributeName("loggerName", compact, str);
        this.checkElementName("Marker", compact, str, true, true);
        this.checkAttributeName("name", compact, str);
        this.checkElementName("Parents", compact, str, false, true);
        this.checkElementName("Message", compact, str, false, true);
        this.checkElementName("Thrown", compact, str, true, true);
        this.checkElementName("Cause", compact, str, true, includeStacktrace);
        this.checkAttributeName("commonElementCount", compact, str);
        this.checkAttributeName("message", compact, str);
        this.checkAttributeName("localizedMessage", compact, str);
        if (includeStacktrace) {
            this.checkElementName("ExtendedStackTrace", compact, str, false, true);
            this.checkAttributeName("class", compact, str);
            this.checkAttributeName("method", compact, str);
            this.checkAttributeName("file", compact, str);
            this.checkAttributeName("line", compact, str);
            this.checkAttributeName("exact", compact, str);
            this.checkAttributeName("location", compact, str);
            this.checkAttributeName("version", compact, str);
        } else {
            this.checkElementNameAbsent("ExtendedStackTrace", compact, str);
        }
        this.checkElementName("Suppressed", compact, str, false, true);
        this.checkAttributeName("loggerFqcn", compact, str);
        this.checkAttributeName("endOfBatch", compact, str);
        if (includeContextMap) {
            this.checkElementName("ContextMap", compact, str, false, true);
        } else {
            this.checkElementNameAbsent("ContextMap", compact, str);
        }
        this.checkElementName("ContextStack", compact, str, false, true);
        if (includeLocationInfo) {
            this.checkElementName("Source", compact, str, true, false);
        } else {
            this.checkElementNameAbsent("Source", compact, str);
        }
        // check some attrs
        this.checkAttribute("loggerFqcn", "f.q.c.n", compact, str);
        this.checkAttribute("loggerName", "a.B", compact, str);
        this.checkJsonPropertyOrder(includeContextStack, includeContextMap, includeStacktrace, str);
    }

    @Test
    public void testContentType() {
        final XmlLayout layout = XmlLayout.createDefaultLayout();
        assertEquals("text/xml; charset=UTF-8", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final XmlLayout layout = XmlLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    public void testExcludeStacktrace() throws Exception {
        this.testAllFeatures(false, false, false, false, false);
    }

    @Test
    public void testIncludeNullDelimiterFalse() throws Exception {
        final AbstractJacksonLayout layout = XmlLayout.newBuilder().setCompact(true).setIncludeNullDelimiter(false)
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertFalse(str.endsWith("\0"));
    }

    @Test
    public void testIncludeNullDelimiterTrue() throws Exception {
        final AbstractJacksonLayout layout = XmlLayout.newBuilder().setCompact(true).setIncludeNullDelimiter(true)
                .build();
        final String str = layout.toSerializable(LogEventFixtures.createLogEvent());
        assertTrue(str.endsWith("\0"));
    }

    /**
     * Test case for MDC conversion pattern.
     */
    @Test
    public void testLayout() throws Exception {
        final Map<String, Appender> appenders = this.rootLogger.getAppenders();
        for (final Appender appender : appenders.values()) {
            this.rootLogger.removeAppender(appender);
        }
        // set up appender
        final XmlLayout layout = XmlLayout.newBuilder().setLocationInfo(true).setProperties(true).setComplete(true)
                .setCompact(false).setIncludeStacktrace(true).build();

        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        this.rootLogger.addAppender(appender);
        this.rootLogger.setLevel(Level.DEBUG);

        // output starting message
        this.rootLogger.debug("starting mdc pattern test");

        this.rootLogger.debug("empty mdc");

        ThreadContext.put("key1", "value1");
        ThreadContext.put("key2", "value2");

        this.rootLogger.debug("filled mdc");

        ThreadContext.remove("key1");
        ThreadContext.remove("key2");

        this.rootLogger.error("finished mdc pattern test", new NullPointerException("test"));

        final Marker marker = MarkerManager.getMarker("EVENT");
        this.rootLogger.error(marker, "marker test");

        appender.stop();

        final List<String> list = appender.getMessages();

        final String string = list.get(0);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", string, "Incorrect header: " + string);
        assertEquals("</Events>", list.get(list.size() - 1), "Incorrect footer");
        this.checkContains("loggerFqcn=\"" + AbstractLogger.class.getName() + "\"", list);
        this.checkContains("level=\"DEBUG\"", list);
        this.checkContains(">starting mdc pattern test</Message>", list);
        // this.checkContains("<Message>starting mdc pattern test</Message>", list);

        // <Marker xmlns="" _class="org.apache.logging.log4j.MarkerManager..Log4jMarker" name="EVENT"/>
        this.checkContains("<Marker", list);
        this.checkContains("name=\"EVENT\"/>", list);

        for (final Appender app : appenders.values()) {
            this.rootLogger.addAppender(app);
        }
    }

    @Test
    public void testLayoutLoggerName() {
        final XmlLayout layout = XmlLayout.newBuilder().setLocationInfo(false).setProperties(true).setComplete(true)
                .setCompact(false).setIncludeStacktrace(true).build();

        final Log4jLogEvent event = Log4jLogEvent.newBuilder() //
                .setLoggerName("a.B") //
                .setLoggerFqcn("f.q.c.n") //
                .setLevel(Level.DEBUG) //
                .setMessage(new SimpleMessage("M")) //
                .setThreadName("threadName") //
                .setTimeMillis(1).build();
        final String str = layout.toSerializable(event);
        assertTrue(str.contains("loggerName=\"a.B\""), str);
    }

    @Test
    public void testLocationOffCompactOffMdcOff() throws Exception {
        this.testAllFeatures(false, false, false, false, true);
    }

    @Test
    public void testLocationOnCompactOnMdcOn() throws Exception {
        this.testAllFeatures(true, true, true, true, true);
    }

    @Test
    public void testLocationOnCompactOnNdcOn() throws Exception {
        this.testAllFeatures(false, false, false, true, false);
    }

    @Test
    public void testStacktraceAsNonString() throws Exception {
        final String str = prepareXMLForStacktraceTests(false);
        assertTrue(str.contains("<ExtendedStackTrace><ExtendedStackTraceItem"), str);
    }

    @Test
    public void testStacktraceAsString() throws Exception {
        final String str = prepareXMLForStacktraceTests(true);
        assertTrue(str.contains("<ExtendedStackTrace>java.lang.NullPointerException"), str);
    }
}
