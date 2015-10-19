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

import java.io.IOException;
import java.util.Set;

import org.apache.james.jmap.api.JmapRequest;
import org.apache.james.jmap.api.JmapResponse;
import org.apache.james.jmap.api.Method;
import org.apache.james.jmap.model.ProtocolRequest;
import org.apache.james.jmap.model.ProtocolResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;

public class RequestHandlerImpl implements RequestHandler {

    private final Set<Method> methods;
    private ObjectMapper objectMapper = new ObjectMapper();

    @VisibleForTesting RequestHandlerImpl(Set<Method> methods) {
        this.methods = methods;
    }

    @Override
    public ProtocolResponse process(ProtocolRequest request) {
        try {
            Method processedMethod = methods.stream()
                .filter(method -> method.methodName().equals(request.getMethod()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("unknown method"));

            processedMethod.process(
                    extractJmapRequest(request, processedMethod.requestClass()));

            JmapResponse jmapResponse = null;
            return formatResponse(request, jmapResponse);
        } catch (IOException e) {
            return new ProtocolResponse("error", new ObjectNode(new JsonNodeFactory(false)).put("type", "Unserializable response"), request.getClientId());
        }
    }

    private <T extends JmapRequest> T extractJmapRequest(ProtocolRequest request, Class<T> requestClass) throws IOException, JsonParseException, JsonMappingException {
        Object obj = objectMapper.readValue(request.getParameters().toString(), new TypeReference<T>() {});
        return (T) obj;
    }

    private ProtocolResponse formatResponse(ProtocolRequest request, JmapResponse jmapResponse) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.pojoNode(jmapResponse);
        return new ProtocolResponse(request.getMethod(), objectNode, request.getClientId());
    }
}
