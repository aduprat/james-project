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

import org.apache.james.jmap.methods.Method;
import org.apache.james.jmap.model.mailbox.Mailbox;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

public class SetMailboxesResponse implements Method.Response {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ImmutableMap.Builder<MailboxCreationId, Mailbox> created;

        private Builder() {
            created = ImmutableMap.builder();
        }

        public Builder created(MailboxCreationId creationId, Mailbox mailbox) {
            created.put(creationId, mailbox);
            return this;
        }

        public Builder created(ImmutableMap<MailboxCreationId, Mailbox> created) {
            this.created.putAll(created);
            return this;
        }

        public SetMailboxesResponse build() {
            return new SetMailboxesResponse(created.build());
        }
    }

    private final ImmutableMap<MailboxCreationId, Mailbox> created;

    private SetMailboxesResponse(ImmutableMap<MailboxCreationId, Mailbox> created) {
        this.created = created;
    }

    public ImmutableMap<MailboxCreationId, Mailbox> getCreated() {
        return created;
    }

    public SetMailboxesResponse.Builder mergeInto(SetMailboxesResponse.Builder responseBuilder) {
        return responseBuilder
            .created(getCreated());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(created);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SetMailboxesResponse) {
            SetMailboxesResponse other = (SetMailboxesResponse) obj;
            return Objects.equal(this.created, other.created);
        }
        return false;
    }
}
