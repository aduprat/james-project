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

package org.apache.james.mailbox.inmemory;

import java.util.List;

import javax.inject.Inject;

import org.apache.james.mailbox.MailboxPathLocker;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.acl.GroupMembershipResolver;
import org.apache.james.mailbox.acl.MailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.mailbox.store.StoreMessageManager;
import org.apache.james.mailbox.store.mail.model.Mailbox;

import com.google.common.collect.Lists;

public class InMemoryMailboxManager extends StoreMailboxManager<InMemoryId> {

    @Inject
    public InMemoryMailboxManager(MailboxSessionMapperFactory<InMemoryId> mailboxSessionMapperFactory, Authenticator authenticator, MailboxPathLocker locker, MailboxACLResolver aclResolver, GroupMembershipResolver groupMembershipResolver) {
        super(mailboxSessionMapperFactory, authenticator, locker, aclResolver, groupMembershipResolver);
    }

    @Override
    public List<Capabilities> getSupportedCapabilities() {
        return Lists.newArrayList(Capabilities.Basic, Capabilities.Move, Capabilities.UserFlags, Capabilities.AttachmentStore);
    }

    @Override
    protected StoreMessageManager<InMemoryId> createMessageManager(Mailbox<InMemoryId> mailbox, MailboxSession session) throws MailboxException {
        return new InMemoryMessageManager(getMapperFactory(),
            getMessageSearchIndex(),
            getEventDispatcher(),
            getLocker(),
            mailbox,
            getAclResolver(),
            getGroupMembershipResolver(),
            getQuotaManager(),
            getQuotaRootResolver());
    }
}
