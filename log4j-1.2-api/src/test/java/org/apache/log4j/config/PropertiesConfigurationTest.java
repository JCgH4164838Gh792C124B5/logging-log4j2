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
package org.apache.log4j.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.DenyAllFilter;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test configuration from Properties.
 */
public class PropertiesConfigurationTest extends AbstractLog4j1ConfigurationTest {

    private static final String TEST_KEY = "log4j.test.tmpdir";
    private static final String SUFFIX = ".properties";

    @Override
    Configuration getConfiguration(String configResourcePrefix) throws URISyntaxException, IOException {
        final String configResource = configResourcePrefix + SUFFIX;
        final InputStream inputStream = ClassLoader.getSystemResourceAsStream(configResource);
        final ConfigurationSource source = new ConfigurationSource(inputStream);
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration configuration = new PropertiesConfigurationFactory().getConfiguration(context, source);
        assertNotNull(configuration, "No configuration created");
        configuration.initialize();
        return configuration;
    }

    @Test
    public void testConfigureNullPointerException() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3247.properties")) {
            // [LOG4J2-3247] configure() should not throw an NPE.
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CONSOLE");
            assertNotNull(appender);
        }
    }

    @Test
    public void testConsoleAppenderFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3247.properties")) {
            // LOG4J2-3281 PropertiesConfiguration.buildAppender not adding filters to appender
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CONSOLE");
            assertNotNull(appender);
            final Filterable filterable = (Filterable) appender;
            final FilterAdapter filter = (FilterAdapter) filterable.getFilter();
            assertNotNull(filter);
            assertTrue(filter.getFilter() instanceof NeutralFilterFixture);
        }
    }

    @Test
    public void testCustomAppenderFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3281.properties")) {
            // LOG4J2-3281 PropertiesConfiguration.buildAppender not adding filters to appender
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CUSTOM");
            assertNotNull(appender);
            final Filterable filterable = (Filterable) appender;
            final FilterAdapter filter = (FilterAdapter) filterable.getFilter();
            assertNotNull(filter);
            assertTrue(filter.getFilter() instanceof NeutralFilterFixture);
        }
    }

    @Test
    public void testConsoleAppenderLevelRangeFilter() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3326.properties")) {
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final Appender appender = configuration.getAppender("CUSTOM");
            assertNotNull(appender);
            final Filterable filterable = (Filterable) appender;
            final CompositeFilter filter = (CompositeFilter) filterable.getFilter();
            final org.apache.logging.log4j.core.Filter[] filters = filter.getFiltersArray();
            final LevelRangeFilter customFilterReal = (LevelRangeFilter) filters[0];
            assertEquals(Level.ALL, customFilterReal.getMinLevel());
            final LevelRangeFilter defaultFilter = (LevelRangeFilter) filters[1];
            assertEquals(Level.TRACE, defaultFilter.getMinLevel());
      }
    }

    @Test
    public void testConfigureAppenderDoesNotExist() throws Exception {
        // Verify that we tolerate a logger which specifies an appender that does not exist.
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/LOG4J2-3407.properties")) {
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
        }
    }

    @Test
    public void testListAppender() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-list.properties")) {
            final Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            final Configuration configuration = loggerContext.getConfiguration();
            final Map<String, Appender> appenders = configuration.getAppenders();
            ListAppender eventAppender = null;
            ListAppender messageAppender = null;
            for (final Map.Entry<String, Appender> entry : appenders.entrySet()) {
                if (entry.getKey().equals("list")) {
                    messageAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                } else if (entry.getKey().equals("events")) {
                    eventAppender = (ListAppender) ((AppenderAdapter.Adapter) entry.getValue()).getAppender();
                }
            }
            assertNotNull(eventAppender, "No Event Appender");
            assertNotNull(messageAppender, "No Message Appender");
            final List<LoggingEvent> events = eventAppender.getEvents();
            assertTrue(events != null && events.size() > 0, "No events");
            final List<String> messages = messageAppender.getMessages();
            assertTrue(messages != null && messages.size() > 0, "No messages");
        }
    }

    @Test
    public void testProperties() throws Exception {
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/log4j1-file-1.properties")) {
            final Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            File file = new File("target/temp.A1");
            assertTrue(file.exists(), "File A1 was not created");
            assertTrue(file.length() > 0, "File A1 is empty");
            file = new File("target/temp.A2");
            assertTrue(file.exists(), "File A2 was not created");
            assertTrue(file.length() > 0, "File A2 is empty");
        }
    }

    @Test
    public void testSystemProperties() throws Exception {
        final String testPathLocation = "target";
        System.setProperty(TEST_KEY, testPathLocation);
        try (LoggerContext loggerContext = TestConfigurator.configure("target/test-classes/config-1.2/log4j-FileAppender-with-props.properties")) {
            // [LOG4J2-3312] Bridge does not convert properties.
            final Configuration configuration = loggerContext.getConfiguration();
            assertNotNull(configuration);
            final String name = "FILE_APPENDER";
            final Appender appender = configuration.getAppender(name);
            assertNotNull(appender, name);
            assertTrue(appender instanceof FileAppender, appender.getClass().getName());
            final FileAppender fileAppender = (FileAppender) appender;
            // Two slashes because that's how the config file is setup.
            assertEquals(testPathLocation + "/hadoop.log", fileAppender.getFileName());
        } finally {
            System.clearProperty(TEST_KEY);
        }
    }


    @Override
    @Test
    public void testConsoleEnhancedPatternLayout() throws Exception {
        super.testConsoleEnhancedPatternLayout();
    }

    @Override
    @Test
    public void testConsoleHtmlLayout() throws Exception {
        super.testConsoleHtmlLayout();
    }

    @Override
    @Test
    public void testConsolePatternLayout() throws Exception {
        super.testConsolePatternLayout();
    }

    @Override
    @Test
    public void testConsoleSimpleLayout() throws Exception {
        super.testConsoleSimpleLayout();
    }

    @Override
    @Test
    public void testFileSimpleLayout() throws Exception {
        super.testFileSimpleLayout();
    }

    @Override
    @Test
    public void testNullAppender() throws Exception {
        super.testNullAppender();
    }

    @Override
    @Test
    public void testConsoleCapitalization() throws Exception {
        super.testConsoleCapitalization();
    }

    @Override
    @Test
    public void testConsoleTtccLayout() throws Exception {
        super.testConsoleTtccLayout();
    }

    @Override
    @Test
    public void testRollingFileAppender() throws Exception {
        super.testRollingFileAppender();
    }

    @Override
    @Test
    public void testDailyRollingFileAppender() throws Exception {
        super.testDailyRollingFileAppender();
    }

    @Override
    @Test
    public void testRollingFileAppenderWithProperties() throws Exception {
        super.testRollingFileAppenderWithProperties();
    }

    @Override
    @Test
    public void testSystemProperties1() throws Exception {
        super.testSystemProperties1();
    }

    @Override
    @Test
    public void testSystemProperties2() throws Exception {
        super.testSystemProperties2();
    }

    @Override
    @Test
    public void testDefaultValues() throws Exception {
        super.testDefaultValues();
    }

    @Override
    @Test
    public void testMultipleFilters(final @TempDir Path tmpFolder) throws Exception {
        super.testMultipleFilters(tmpFolder);
    }

    @Test
    public void testUntrimmedValues() throws Exception {
        try {
            final Configuration config = getConfiguration("config-1.2/log4j-untrimmed");
            final LoggerConfig rootLogger = config.getRootLogger();
            assertEquals(Level.DEBUG, rootLogger.getLevel());
            final Appender appender = config.getAppender("Console");
            assertTrue(appender instanceof ConsoleAppender);
            final Layout layout = appender.getLayout();
            assertTrue(layout instanceof PatternLayout);
            assertEquals("%v1Level - %m%n", ((PatternLayout)layout).getConversionPattern());
            final Filter filter = ((Filterable) appender).getFilter();
            assertTrue(filter instanceof DenyAllFilter);
            config.start();
            config.stop();
        } catch (NoClassDefFoundError e) {
            fail(e.getMessage());
        }
    }

    @Override
    @Test
    public void testGlobalThreshold() throws Exception {
        super.testGlobalThreshold();
    }

}
