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

import org.apache.logging.log4j.status.StatusLogger;

/**
 * Creates {@link FormattedMessage} instances for {@link MessageFactory} methods.
 * <p>
 * Creates {@link SimpleMessage} objects that do not retain a reference to the parameter object.
 * </p>
 * <p>
 * Intended for use by the {@link StatusLogger}: this logger retains a queue of recently logged messages in memory,
 * causing memory leaks in web applications. (LOG4J2-1176)
 * </p>
 * <p>
 * This class is immutable.
 * </p>
 * <h3></h3>
 * <h4>Note to implementors</h4>
 * <p>
 * This class does <em>not</em> implement any {@link MessageFactory} methods and lets the superclass funnel those calls
 * through {@link #newMessage(String, Object...)}.
 * </p>
 */
public final class ParameterizedNoReferenceMessageFactory implements MessageFactory {

    /**
     * Message implementation that only keeps a reference to the error text and the error (if any), not to the
     * message parameters, in order to avoid memory leaks. This addresses LOG4J2-1368.
     * @since 2.6
     */
    static class StatusMessage implements Message {
        private final String formattedMessage;
        private final Throwable throwable;

        public StatusMessage(final String formattedMessage, final Throwable throwable) {
            this.formattedMessage = formattedMessage;
            this.throwable = throwable;
        }

        @Override
        public String getFormattedMessage() {
            return formattedMessage;
        }

        @Override
        public String getFormat() {
            return formattedMessage;
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public Throwable getThrowable() {
            return throwable;
        }
    }

    /**
     * Constructs a message factory with default flow strings.
     */
    public ParameterizedNoReferenceMessageFactory() {
        super();
    }

    /**
     * Instance of ParameterizedStatusMessageFactory.
     */
    public static final ParameterizedNoReferenceMessageFactory INSTANCE = new ParameterizedNoReferenceMessageFactory();

    /**
     * Creates {@link SimpleMessage} instances containing the formatted parameterized message string.
     *
     * @param message The message pattern.
     * @param params The message parameters.
     * @return The Message.
     *
     * @see MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String message, final Object... params) {
        if (params == null) {
            return new SimpleMessage(message);
        }
        final ParameterizedMessage msg = new ParameterizedMessage(message, params);
        return new StatusMessage(msg.getFormattedMessage(), msg.getThrowable());
    }
}
