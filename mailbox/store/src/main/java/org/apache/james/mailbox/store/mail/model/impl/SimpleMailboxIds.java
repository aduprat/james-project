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

package org.apache.james.mailbox.store.mail.model.impl;

import java.util.Set;

import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.MailboxIds;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public class SimpleMailboxIds<Id extends MailboxId> implements MailboxIds<Id> {

    private final Set<Id> mailboxIds;

    public SimpleMailboxIds(Set<Id> mailboxIds) {
        Preconditions.checkNotNull(mailboxIds, "'mailboxIds' is mandatory");
        this.mailboxIds = mailboxIds;
    }

    @Override
    public Set<Id> mailboxIds() {
        return mailboxIds;
    }

    @Override
    public MailboxIds<Id> add(Id id) {
        return new SimpleMailboxIds<Id>(ImmutableSet.<Id> builder()
                .addAll(mailboxIds)
                .add(id)
                .build());
    }

    @Override
    public String serialize() {
        return "(" + Joiner.on(", ").join(mailboxIds) + ")";
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj instanceof SimpleMailboxIds) {
            SimpleMailboxIds<Id> other = (SimpleMailboxIds<Id>) obj;
            return Sets.symmetricDifference(this.mailboxIds, other.mailboxIds).isEmpty();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mailboxIds.hashCode();
    }

    @Override
    public String toString() {
        return serialize();
    }
}
