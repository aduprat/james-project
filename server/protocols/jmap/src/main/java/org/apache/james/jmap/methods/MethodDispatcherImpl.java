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

import java.util.Set;

import org.apache.james.jmap.api.JmapRequest;
import org.apache.james.jmap.api.Method;
import org.apache.james.jmap.model.Protocol;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MethodDispatcherImpl {

    private final Set<Method<JmapRequest>> methods;
    private ObjectMapper objectMapper = new ObjectMapper();

    private MethodDispatcherImpl(Set<Method<JmapRequest>> methods) {
        this.methods = methods;
    }

    public void process(Protocol request) {
        Method<JmapRequest> requestMethod = methods.stream()
            .filter(method -> method.methodName().equals(request.getMethod()))
            .findAny()
            .orElseThrow(() -> new IllegalStateException("unknown method"));
        requestMethod.process(objectMapper.readValue(request.getJson(), JmapRequest.class));
    }
}
