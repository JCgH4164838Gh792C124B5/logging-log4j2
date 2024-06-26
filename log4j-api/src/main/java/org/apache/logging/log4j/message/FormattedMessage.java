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

import java.text.Format;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Handles messages that contain a format String. This converts each message into a {@link MessageFormatMessage},
 * {@link StringFormattedMessage}, or {@link ParameterizedMessage} if the {@linkplain #getFormat() format}
 * conforms to {@link MessageFormat}, {@link String#format(String, Object...)}, or anything else respectively.
 */
public class FormattedMessage implements Message {

    private static final String FORMAT_SPECIFIER = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
    private static final Pattern MSG_PATTERN = Pattern.compile(FORMAT_SPECIFIER);

    private final String messagePattern;
    private final Object[] argArray;
    private String formattedMessage;
    private final Throwable throwable;
    private Message message;
    private final Locale locale;

    /**
     * Constructs with a locale, a pattern and a single parameter.
     * @param locale The locale
     * @param messagePattern The message pattern.
     * @param arg The parameter.
     * @since 2.6
     */
    public FormattedMessage(final Locale locale, final String messagePattern, final Object arg) {
        this(locale, messagePattern, new Object[] { arg }, null);
    }

    /**
     * Constructs with a locale, a pattern and two parameters.
     * @param locale The locale
     * @param messagePattern The message pattern.
     * @param arg1 The first parameter.
     * @param arg2 The second parameter.
     * @since 2.6
     */
    public FormattedMessage(final Locale locale, final String messagePattern, final Object arg1, final Object arg2) {
        this(locale, messagePattern, new Object[] { arg1, arg2 });
    }

    /**
     * Constructs with a locale, a pattern and a parameter array.
     * @param locale The locale
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     * @since 2.6
     */
    public FormattedMessage(final Locale locale, final String messagePattern, final Object... arguments) {
        this(locale, messagePattern, arguments, null);
    }

    /**
     * Constructs with a locale, a pattern, a parameter array, and a throwable.
     * @param locale The Locale
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     * @param throwable The throwable
     * @since 2.6
     */
    public FormattedMessage(final Locale locale, final String messagePattern, final Object[] arguments, final Throwable throwable) {
        this.locale = locale;
        this.messagePattern = messagePattern;
        this.argArray = arguments;
        this.throwable = throwable;
    }

    /**
     * Constructs with a pattern and a single parameter.
     * @param messagePattern The message pattern.
     * @param arg The parameter.
     */
    public FormattedMessage(final String messagePattern, final Object arg) {
        this(messagePattern, new Object[] { arg }, null);
    }

    /**
     * Constructs with a pattern and two parameters.
     * @param messagePattern The message pattern.
     * @param arg1 The first parameter.
     * @param arg2 The second parameter.
     */
    public FormattedMessage(final String messagePattern, final Object arg1, final Object arg2) {
        this(messagePattern, new Object[] { arg1, arg2 });
    }

    /**
     * Constructs with a pattern and a parameter array.
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     */
    public FormattedMessage(final String messagePattern, final Object... arguments) {
        this(messagePattern, arguments, null);
    }

    /**
     * Constructs with a pattern, a parameter array, and a throwable.
     * @param messagePattern The message pattern.
     * @param arguments The parameter.
     * @param throwable The throwable
     */
    public FormattedMessage(final String messagePattern, final Object[] arguments, final Throwable throwable) {
        this.locale = Locale.getDefault(Locale.Category.FORMAT);
        this.messagePattern = messagePattern;
        this.argArray = arguments;
        this.throwable = throwable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormattedMessage that = (FormattedMessage) o;
        return Objects.equals(messagePattern, that.messagePattern) && Arrays.equals(argArray, that.argArray);
    }

    /**
     * Gets the message pattern.
     * @return the message pattern.
     */
    @Override
    public String getFormat() {
        return messagePattern;
    }

    /**
     * Gets the formatted message.
     * @return the formatted message.
     */
    @Override
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            if (message == null) {
                message = getMessage(messagePattern, argArray, throwable);
            }
            formattedMessage = message.getFormattedMessage();
        }
        return formattedMessage;
    }

    protected Message getMessage(final String msgPattern, final Object[] args, final Throwable aThrowable) {
        try {
            final MessageFormat format = new MessageFormat(msgPattern);
            final Format[] formats = format.getFormats();
            if (formats.length > 0) {
                return new MessageFormatMessage(locale, msgPattern, args);
            }
        } catch (final Exception ignored) {
            // Obviously, the message is not a proper pattern for MessageFormat.
        }
        try {
            if (MSG_PATTERN.matcher(msgPattern).find()) {
                return new StringFormattedMessage(locale, msgPattern, args);
            }
        } catch (final Exception ignored) {
            // Also not properly formatted.
        }
        return new ParameterizedMessage(msgPattern, args, aThrowable);
    }

    /**
     * Gets the message parameters.
     * @return the message parameters.
     */
    @Override
    public Object[] getParameters() {
        return argArray;
    }

    @Override
    public Throwable getThrowable() {
        if (throwable != null) {
            return throwable;
        }
        if (message == null) {
            message = getMessage(messagePattern, argArray, null);
        }
        return message.getThrowable();
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(messagePattern);
        result = 31 * result + Arrays.hashCode(argArray);
        return result;
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

}
