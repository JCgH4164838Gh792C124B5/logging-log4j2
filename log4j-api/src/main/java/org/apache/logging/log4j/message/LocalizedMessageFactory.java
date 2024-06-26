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

import java.util.ResourceBundle;

/**
 * Creates {@link FormattedMessage} instances for {@link MessageFactory} methods.
 *
 * <h3></h3>
 * <h4>Note to implementors</h4>
 * <p>
 * This class does <em>not</em> implement any {@link MessageFactory} methods and lets the superclass funnel those calls
 * through {@link #newMessage(String, Object...)}.
 * </p>
 */
public class LocalizedMessageFactory implements MessageFactory {

    private final ResourceBundle resourceBundle;
    private final String baseName;

    public LocalizedMessageFactory(final ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        this.baseName = null;
    }

    public LocalizedMessageFactory(final String baseName) {
        this.resourceBundle = null;
        this.baseName = baseName;
    }

    /**
     * Gets the resource bundle base name if set.
     *
     * @return the resource bundle base name if set. May be null.
     */
    public String getBaseName() {
        return this.baseName;
    }

    /**
     * Gets the resource bundle if set.
     *
     * @return the resource bundle if set. May be null.
     */
    public ResourceBundle getResourceBundle() {
        return this.resourceBundle;
    }

    /**
     * @since 2.8
     */
    @Override
    public Message newMessage(final String key) {
        if (resourceBundle == null) {
            return new LocalizedMessage(baseName,  key, null);
        }
        return new LocalizedMessage(resourceBundle, key);
    }

    /**
     * Creates {@link LocalizedMessage} instances.
     *
     * @param key The key String, used as a message if the key is absent.
     * @param params The parameters for the message at the given key.
     * @return The LocalizedMessage.
     *
     * @see org.apache.logging.log4j.message.MessageFactory#newMessage(String, Object...)
     */
    @Override
    public Message newMessage(final String key, final Object... params) {
        if (resourceBundle == null) {
            return new LocalizedMessage(baseName, key, params);
        }
        return new LocalizedMessage(resourceBundle, key, params);
    }

}
