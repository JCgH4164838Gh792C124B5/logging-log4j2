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
package org.apache.logging.log4j.jackson;

import org.apache.logging.log4j.Marker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Jackson mix-in for {@link Marker}.
 * <p>
 * If we want to deal with more than one {@link Marker} implementation then recode these annotations to include metadata.
 * </p>
 * <p>
 * <em>Consider this class private.</em>
 * </p>
 * <p>
 * Example XML:
 * </p>
 * <pre>
&lt;Marker name=&quot;Marker1&quot;&gt;
    &lt;Parents&gt;
        &lt;Marker name=&quot;ParentMarker1&quot;&gt;
            &lt;Parents&gt;
                &lt;Marker name=&quot;GrandMotherMarker&quot;/&gt;
                &lt;Marker name=&quot;GrandFatherMarker&quot;/&gt;
            &lt;/Parents&gt;
        &lt;/Marker&gt;
        &lt;Marker name=&quot;ParentMarker2&quot;/&gt;
    &lt;/Parents&gt;
&lt;/Marker&gt;
 * </pre>
 *
 * @see Marker
 */
// Alternate for multiple Marker implementation.
// @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(as = org.apache.logging.log4j.MarkerManager.Log4jMarker.class)
public abstract class MarkerMixIn implements Marker {

    @JsonCreator
    public MarkerMixIn(@JsonProperty("name") final String name) {
        // empty
    }

    @Override
    @JsonProperty("name")
    public abstract String getName();

    @Override
    @JsonProperty(JsonConstants.ELT_PARENTS)
    public abstract Marker[] getParents();

}
