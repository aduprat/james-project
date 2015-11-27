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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class GetMessagesRequestTest {

    @Test
    public void shouldAllowOptionalAccountId() {
        GetMessagesRequest result = GetMessagesRequest.builder().messages(MessageId.of("user-inbox-1")).properties(Property.id).build();
        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEmpty();
    }

    @Test
    public void shouldThrowWhenAccountIdIsNull() {
        assertThatThrownBy(() -> GetMessagesRequest.builder().accountId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldAllowEmptyMessagesList() {
        GetMessagesRequest result = GetMessagesRequest.builder().accountId("accountId").messages().properties(Property.id).build();
        assertThat(result).isNotNull();
        assertThat(result.getMessageIds()).isEmpty();
    }

    @Test
    public void shouldAllowAbsentPropertyList() {
        GetMessagesRequest result = GetMessagesRequest.builder().accountId("accountId").messages().build();
        assertThat(result).isNotNull();
        assertThat(result.getProperties()).isEmpty();
    }

    @Test
    public void shouldAllowEmptyPropertyList() {
        GetMessagesRequest result = GetMessagesRequest.builder().accountId("accountId").messages().properties(new Property[0]).build();
        assertThat(result).isNotNull();
        assertThat(result.getProperties()).contains(ImmutableList.of());
    }
}
