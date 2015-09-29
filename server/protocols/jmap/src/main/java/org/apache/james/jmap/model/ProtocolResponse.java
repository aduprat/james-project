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
package org.apache.james.jmap.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProtocolResponse {

    private final String method;
    private final ObjectNode json;
    private final String clientId;

    public ProtocolResponse(String method, ObjectNode json, String clientId) {
        this.method = method;
        this.json = json;
        this.clientId = clientId;
    }

    public String getMethod() {
        return method;
    }

    public ObjectNode getJson() {
        return json;
    }

    public String getClientId() {
        return clientId;
    }

    public Object[] asProtocolSpecification() {
        return new Object[] { getMethod(), getJson(), getClientId() };
    }
}