/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

public class JsonableTest {

    static final String JSON_TYPE = "@type";

    @Test
    public void serializeShouldWork() throws Exception {
        MyClass myClass = new MyClass("bar");
        
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(myClass);
        
        String expectedJson = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClass\",\"foo\":\"bar\"}";
        
        assertThat(json).isEqualTo(expectedJson);
    }

    @Test
    public void deserializeShouldWork() throws Exception {
        String json = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClass\",\"foo\":\"bar\"}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        String type = jsonNode.get("@type").asText();

        Class<?> clazz = Class.forName(type);

        MyClass myClass = (MyClass) objectMapper.convertValue(jsonNode, clazz);
        assertThat(myClass.foo).isEqualTo("bar");
    }

    @Test
    public void chainingShouldWork() throws Exception {
        MyClass expected = new MyClass("bar");
        
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(expected);
        
        JsonNode jsonNode = objectMapper.readTree(json);

        String type = jsonNode.get("@type").asText();

        Class<?> clazz = Class.forName(type);

        MyClass myClass = (MyClass) objectMapper.convertValue(jsonNode, clazz);
        assertThat(myClass.foo).isEqualTo("bar");
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = JSON_TYPE)
    private static class MyClass {

        @JsonProperty
        private final String foo;

        @JsonCreator
        public MyClass(@JsonProperty("foo") String foo) {
            this.foo = foo;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(foo);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MyClass) {
                MyClass other = (MyClass) obj;
                return Objects.equals(foo, other.foo);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(MyClass.class)
                    .add("foo", foo)
                    .toString();
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = JSON_TYPE)
    private static class MyClassWithMap {

        @JsonProperty
        private final Map<Integer, String> foo;

        @JsonCreator
        public MyClassWithMap(@JsonProperty("foo") Map<Integer, String> foo) {
            this.foo = foo;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(foo);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MyClass) {
                MyClass other = (MyClass) obj;
                return Objects.equals(foo, other.foo);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(MyClass.class)
                    .add("foo", foo)
                    .toString();
        }
    }

    @Test
    public void serializeMapAsObjectShouldWork() throws Exception {
        MyClassWithMap myClass = new MyClassWithMap(ImmutableMap.of(1, "bar", 2, "bar2"));
        
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(myClass);
        
        String expectedJson = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClassWithMap\",\"foo\":{\"1\":\"bar\",\"2\":\"bar2\"}}";
        
        assertThat(json).isEqualTo(expectedJson);
    }

    @Test
    public void deserializeMapAsObjectShouldWork() throws Exception {
        String json = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClassWithMap\",\"foo\":{\"1\":\"bar\",\"2\":\"bar2\"}}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        String type = jsonNode.get("@type").asText();

        Class<?> clazz = Class.forName(type);

        MyClassWithMap myClass = (MyClassWithMap) objectMapper.convertValue(jsonNode, clazz);
        assertThat(myClass.foo).containsAllEntriesOf(ImmutableMap.of(1, "bar", 2, "bar2"));
    }

    @Test
    public void serializeMapShouldWork() throws Exception {
        HashMap<Integer,String> myClass = new HashMap<>();
        myClass.put(1, "bar");
        myClass.put(2, "bar2");
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        String json = objectMapper.writeValueAsString(myClass);
        
        String expectedJson = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClassWithMap\",{\"1\":\"bar\",\"2\":\"bar2\"}}";
        
        assertThat(json).isEqualTo(expectedJson);
    }

    @Test
    public void deserializeMapShouldWork() throws Exception {
        String json = "{\"@type\":\"org.apache.james.queue.rabbitmq.JsonableTest$MyClassWithMap\",\"foo\":{\"1\":\"bar\",\"2\":\"bar2\"}}";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        String type = jsonNode.get("@type").asText();

        Class<?> clazz = Class.forName(type);

        MyClassWithMap myClass = (MyClassWithMap) objectMapper.convertValue(jsonNode, clazz);
        assertThat(myClass.foo).containsAllEntriesOf(ImmutableMap.of(1, "bar", 2, "bar2"));
    }
}
