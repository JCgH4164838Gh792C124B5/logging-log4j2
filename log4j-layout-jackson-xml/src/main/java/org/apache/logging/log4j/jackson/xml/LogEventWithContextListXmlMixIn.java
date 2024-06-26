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
package org.apache.logging.log4j.jackson.xml;

import org.apache.logging.log4j.jackson.ContextDataAsEntryListDeserializer;
import org.apache.logging.log4j.jackson.XmlConstants;
import org.apache.logging.log4j.util.ReadOnlyStringMap;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * <pre>
 * AbstractLogEventMixIn
*├─ AbstractLogEventXmlMixIn
*├──── LogEventWithContextListXmlMixIn
*├──── LogEventWithContextMapXmlMixIn
*├─ JsonLogEventMixIn
*├──── JsonLogEventWithContextListMixIn
*├──── JsonLogEventWithContextMapMixIn
 * </pre>
 */
public abstract class LogEventWithContextListXmlMixIn extends AbstractLogEventXmlMixIn {

    @JacksonXmlProperty(namespace = XmlConstants.XML_NAMESPACE, localName = XmlConstants.ELT_CONTEXT_MAP)
    @JsonSerialize(using = ContextDataAsEntryListXmlSerializer.class)
    @JsonDeserialize(using = ContextDataAsEntryListDeserializer.class)
    @Override
    public abstract ReadOnlyStringMap getContextData();

}
