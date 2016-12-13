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

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Random;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxMetaData;
import org.apache.james.mailbox.model.MailboxQuery;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.MessageIdManagerTestSystem;
import org.apache.james.mailbox.store.mail.model.Mailbox;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;

public class InMemoryMessageIdManagerTestSystem extends MessageIdManagerTestSystem {

    private static final Random RANDOM = new Random();

    private final MailboxManager mailboxManager;
    private final MailboxSession mailboxSession;

    public InMemoryMessageIdManagerTestSystem(MailboxManager mailboxManager, MailboxSession mailboxSession, 
            Mailbox mailbox1, Mailbox mailbox2, Mailbox mailbox3) {
        super(new InMemoryMessageIdManager(mailboxManager), mailboxSession, mailbox1, mailbox2, mailbox3);
        this.mailboxManager = mailboxManager;
        this.mailboxSession = mailboxSession;
    }

    @Override
    public MessageId persist(MailboxId mailboxId, Flags flags) {
        try {
            MessageManager messageManager = mailboxManager.getMailbox(mailboxId, mailboxSession);
            return messageManager.appendMessage(new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), mailboxSession, false, flags)
                    .getMessageId();
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public MessageId createNotUsedMessageId() {
        return InMemoryMessageId.of(RANDOM.nextLong());
    }

    @Override
    public void deleteMailbox(final MailboxId mailboxId) {
        try {
            Optional<MailboxMetaData> mailbox = retrieveMailbox(mailboxId);
            if (mailbox.isPresent()) {
                mailboxManager.deleteMailbox(mailbox.get().getPath(), mailboxSession);
            }
        } catch (MailboxException e) {
            Throwables.propagate(e);
        }
    }

    private Optional<MailboxMetaData> retrieveMailbox(final MailboxId mailboxId) throws MailboxException {
        MailboxQuery userMailboxesQuery = MailboxQuery.builder(mailboxSession).expression("*").build();
        return FluentIterable.from(mailboxManager.search(userMailboxesQuery, mailboxSession))
            .filter(new Predicate<MailboxMetaData>() {

                @Override
                public boolean apply(MailboxMetaData mailboxMetaData) {
                    return mailboxMetaData.getId().equals(mailboxId);
                }
            })
            .first();
    }

    @Override
    public void clean() {

    }

}
