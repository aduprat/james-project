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

import java.util.Arrays;
import java.util.Optional;

import org.apache.james.jmap.methods.JmapRequest;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.collect.ImmutableList;

@JsonDeserialize(builder = GetMessagesRequest.Builder.class)
public class GetMessagesRequest implements JmapRequest {

    public static Builder builder() {
        return new Builder();
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        
        private Optional<String> accountId;
        private ImmutableList.Builder<MessageId> messages;
        private Optional<ImmutableList<Property>> properties;

        private Builder() {
            accountId = Optional.empty();
            messages = ImmutableList.builder();
            properties = Optional.empty();
        }
        
        public Builder accountId(String accountId) {
            this.accountId = Optional.of(accountId);
            return this;
        }

        public Builder messages(MessageId... messages) {
            this.messages.addAll(Arrays.asList(messages));
            return this;
        }

        public Builder properties(Property... properties) {
            this.properties = Optional.of(ImmutableList.copyOf(properties));
            return this;
        }
        
        public GetMessagesRequest build() {
            return new GetMessagesRequest(accountId, messages.build(), properties);
        }
    }

    private final Optional<String> accountId;
    private final ImmutableList<MessageId> messages;
    private final Optional<ImmutableList<Property>> properties;

    public GetMessagesRequest(Optional<String> accountId, ImmutableList<MessageId> messages, Optional<ImmutableList<Property>> properties) {
        this.accountId = accountId;
        this.messages = messages;
        this.properties = properties;
    }
    
    public Optional<String> getAccountId() {
        return accountId;
    }
    
    public ImmutableList<MessageId> getMessageIds() {
        return messages;
    }
    
    public Optional<ImmutableList<Property>> getProperties() {
        return properties;
    }
}
