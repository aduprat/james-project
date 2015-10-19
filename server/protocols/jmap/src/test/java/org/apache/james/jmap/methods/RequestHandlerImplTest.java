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

package org.apache.james.jmap.methods;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.jmap.api.JmapRequest;
import org.apache.james.jmap.api.JmapResponse;
import org.apache.james.jmap.api.Method;
import org.apache.james.jmap.model.ProtocolRequest;
import org.apache.james.jmap.model.ProtocolResponse;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;

public class RequestHandlerImplTest {

    @Test(expected=IllegalStateException.class)
    public void processShouldThrowWhenUnknownMethod() {
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("getAccounts"),
                new ObjectNode(new JsonNodeFactory(false)).putObject("{\"id\": \"id\"}"),
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        RequestHandlerImpl requestHandlerImpl = new RequestHandlerImpl(ImmutableSet.of());
        requestHandlerImpl.process(ProtocolRequest.deserialize(nodes));
    }

    @Test
    public void processShouldWorkWhenKnownMethod() {
        ObjectNode parameters = new ObjectNode(new JsonNodeFactory(false));
        parameters.put("id", "myId");
        parameters.put("name", "myName");
        
        JsonNode[] nodes = new JsonNode[] { new ObjectNode(new JsonNodeFactory(false)).textNode("myMethod"),
                parameters,
                new ObjectNode(new JsonNodeFactory(false)).textNode("#1")} ;

        RequestHandlerImpl requestHandlerImpl = new RequestHandlerImpl(ImmutableSet.of(new MyMethod()));
        ProtocolResponse response = requestHandlerImpl.process(ProtocolRequest.deserialize(nodes));

        assertThat(response.getResults().findValue("id")).isEqualTo("myId");
        assertThat(response.getResults().findValue("name")).isEqualTo("myName");
        assertThat(response.getResults().findValue("message")).isEqualTo("works");
    }

    public class MyJmapRequest implements JmapRequest {

        public String id;
        public String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    private class MyJmapResponse implements JmapResponse {

        private final String id;
        private final String name;
        private final String message;

        public MyJmapResponse(String id, String name, String message) {
            this.id = id;
            this.name = name;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getMessage() {
            return message;
        }
    }

    private class MyMethod implements Method<MyJmapRequest, MyJmapResponse> {

        @Override
        public String methodName() {
            return "myMethod";
        }

        @Override
        public MyJmapResponse process(MyJmapRequest request) {
            return new MyJmapResponse(request.getId(), request.getName(), "works");
        }

        @Override
        public Class<MyJmapRequest> requestClass() {
            return MyJmapRequest.class;
        }

        @Override
        public Class<MyJmapResponse> responseClass() {
            return MyJmapResponse.class;
        }
        
    }
}
