/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.jeromq.appender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.Strings;

/**
 * Sends log events to one or more ZeroMQ (JeroMQ) endpoints.
 * <p>
 * Requires the JeroMQ jar (LGPL as of 0.3.5)
 * </p>
 */
// TODO
// Some methods are synchronized because a ZMQ.Socket is not thread-safe
// Using a ThreadLocal for the publisher hangs tests on shutdown. There must be
// some issue on threads owning certain resources as opposed to others.
@Configurable(elementType = Appender.ELEMENT_TYPE, printObject = true)
@Plugin("JeroMQ")
public final class JeroMqAppender extends AbstractAppender {

    private static final int DEFAULT_BACKLOG = 100;

    private static final int DEFAULT_IVL = 100;

    private static final int DEFAULT_RCV_HWM = 1000;

    private static final int DEFAULT_SND_HWM = 1000;

    private final JeroMqManager manager;
    private final List<String> endpoints;
    private int sendRcFalse;
    private int sendRcTrue;

    private JeroMqAppender(final String name, final Filter filter, final Layout layout,
            final boolean ignoreExceptions, final List<String> endpoints, final long affinity, final long backlog,
            final boolean delayAttachOnConnect, final byte[] identity, final boolean ipv4Only, final long linger,
            final long maxMsgSize, final long rcvHwm, final long receiveBufferSize, final int receiveTimeOut,
            final long reconnectIVL, final long reconnectIVLMax, final long sendBufferSize, final int sendTimeOut,
            final long sndHWM, final int tcpKeepAlive, final long tcpKeepAliveCount, final long tcpKeepAliveIdle,
            final long tcpKeepAliveInterval, final boolean xpubVerbose, Property[] properties) {
        super(name, filter, layout, ignoreExceptions, properties);
        this.manager = JeroMqManager.getJeroMqManager(name, affinity, backlog, delayAttachOnConnect, identity, ipv4Only,
            linger, maxMsgSize, rcvHwm, receiveBufferSize, receiveTimeOut, reconnectIVL, reconnectIVLMax,
            sendBufferSize, sendTimeOut, sndHWM, tcpKeepAlive, tcpKeepAliveCount, tcpKeepAliveIdle,
            tcpKeepAliveInterval, xpubVerbose, endpoints);
        this.endpoints = endpoints;
    }

    // The ZMQ.Socket class has other set methods that we do not cover because
    // they throw unsupported operation exceptions.
    @PluginFactory
    public static JeroMqAppender createAppender(
            // @formatter:off
            @Required(message = "No name provided for JeroMqAppender") @PluginAttribute final String name,
            @PluginElement Layout layout,
            @PluginElement final Filter filter,
            @PluginElement final Property[] properties,
            // Super attributes
            @PluginAttribute final boolean ignoreExceptions,
            // ZMQ attributes; defaults picked from zmq.Options.
            @PluginAttribute(defaultLong = 0) final long affinity,
            @PluginAttribute(defaultLong = DEFAULT_BACKLOG) final long backlog,
            @PluginAttribute final boolean delayAttachOnConnect,
            @PluginAttribute final byte[] identity,
            @PluginAttribute(defaultBoolean = true) final boolean ipv4Only,
            @PluginAttribute(defaultLong = -1) final long linger,
            @PluginAttribute(defaultLong = -1) final long maxMsgSize,
            @PluginAttribute(defaultLong = DEFAULT_RCV_HWM) final long rcvHwm,
            @PluginAttribute(defaultLong = 0) final long receiveBufferSize,
            @PluginAttribute(defaultLong = -1) final int receiveTimeOut,
            @PluginAttribute(defaultLong = DEFAULT_IVL) final long reconnectIVL,
            @PluginAttribute(defaultLong = 0) final long reconnectIVLMax,
            @PluginAttribute(defaultLong = 0) final long sendBufferSize,
            @PluginAttribute(defaultLong = -1) final int sendTimeOut,
            @PluginAttribute(defaultLong = DEFAULT_SND_HWM) final long sndHwm,
            @PluginAttribute(defaultInt = -1) final int tcpKeepAlive,
            @PluginAttribute(defaultLong = -1) final long tcpKeepAliveCount,
            @PluginAttribute(defaultLong = -1) final long tcpKeepAliveIdle,
            @PluginAttribute(defaultLong = -1) final long tcpKeepAliveInterval,
            @PluginAttribute final boolean xpubVerbose
            // @formatter:on
    ) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        List<String> endpoints;
        if (properties == null) {
            endpoints = new ArrayList<>(0);
        } else {
            endpoints = new ArrayList<>(properties.length);
            for (final Property property : properties) {
                if ("endpoint".equalsIgnoreCase(property.getName())) {
                    final String value = property.getValue();
                    if (Strings.isNotEmpty(value)) {
                        endpoints.add(value);
                    }
                }
            }
        }
        LOGGER.debug("Creating JeroMqAppender with name={}, filter={}, layout={}, ignoreExceptions={}, endpoints={}",
                name, filter, layout, ignoreExceptions, endpoints);
        return new JeroMqAppender(name, filter, layout, ignoreExceptions, endpoints, affinity, backlog,
                delayAttachOnConnect, identity, ipv4Only, linger, maxMsgSize, rcvHwm, receiveBufferSize,
                receiveTimeOut, reconnectIVL, reconnectIVLMax, sendBufferSize, sendTimeOut, sndHwm, tcpKeepAlive,
                tcpKeepAliveCount, tcpKeepAliveIdle, tcpKeepAliveInterval, xpubVerbose, Property.EMPTY_ARRAY);
    }

    @Override
    public synchronized void append(final LogEvent event) {
        final Layout layout = getLayout();
        final byte[] formattedMessage = layout.toByteArray(event);
        if (manager.send(getLayout().toByteArray(event))) {
            sendRcTrue++;
        } else {
            sendRcFalse++;
            LOGGER.error("Appender {} could not send message {} to JeroMQ {}", getName(), sendRcFalse, formattedMessage);
        }
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
    }

    // not public, handy for testing
    int getSendRcFalse() {
        return sendRcFalse;
    }

    // not public, handy for testing
    int getSendRcTrue() {
        return sendRcTrue;
    }

    // not public, handy for testing
    void resetSendRcs() {
        sendRcTrue = sendRcFalse = 0;
    }

    // not public, handy for testing
    byte[] recv(final int timeoutMs) {
        return manager.recv(timeoutMs);
    }

    @Override
    public String toString() {
        return "JeroMqAppender{" +
            "name=" + getName() +
            ", state=" + getState() +
            ", manager=" + manager +
            ", endpoints=" + endpoints +
            '}';
    }
}
