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

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jmap.model.MailboxCreationId;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMailboxesRequest;
import org.apache.james.jmap.model.SetMailboxesResponse;
import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.MailboxRequest;
import org.apache.james.jmap.utils.MailboxHierarchySorter;
import org.apache.james.jmap.utils.MailboxUtils;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.MailboxId;

import com.google.common.annotations.VisibleForTesting;

public class SetMailboxesCreationProcessor<Id extends MailboxId> implements SetMailboxesProcessor<Id> {

    private final MailboxManager mailboxManager;
    private final MailboxHierarchySorter<Map.Entry<MailboxCreationId, MailboxRequest>, String> mailboxHierarchySorter;
    private final MailboxUtils<Id> mailboxUtils;

    @Inject
    @VisibleForTesting
    SetMailboxesCreationProcessor(MailboxManager mailboxManager, MailboxUtils<Id> mailboxUtils) {
        this.mailboxManager = mailboxManager;
        this.mailboxHierarchySorter =
            new MailboxHierarchySorter<>(
                x -> x.getKey().getCreationId(),
                x -> x.getValue().getParentId());
        this.mailboxUtils = mailboxUtils;
    }

    public SetMailboxesResponse process(SetMailboxesRequest request, MailboxSession mailboxSession) {
        SetMailboxesResponse.Builder builder = SetMailboxesResponse.builder();
        mailboxHierarchySorter.sortFromRootToLeaf(request.getCreate().entrySet())
            .forEach(entry -> createMailbox(entry.getKey(), entry.getValue(), mailboxSession, builder));
        return builder.build();
    }

    private void createMailbox(MailboxCreationId mailboxCreationId, MailboxRequest mailboxRequest, MailboxSession mailboxSession,
            SetMailboxesResponse.Builder builder) {
        try {
            MailboxPath mailboxPath = getMailboxPath(mailboxRequest, mailboxSession);
            System.out.println("mailboxPath to create: " + mailboxPath);
            mailboxManager.createMailbox(mailboxPath,
                    mailboxSession);
            Optional<Mailbox> mailbox = mailboxUtils.mailboxFromMailboxPath(mailboxPath, mailboxSession);
            if (mailbox.isPresent()) {
                builder.creation(mailboxCreationId, mailbox.get());
            } else {
                builder.notCreated(mailboxCreationId, SetError.builder()
                        .description("Error when creating the mailbox")
                        .build());
            }
        } catch (MailboxException e) {
            builder.notCreated(mailboxCreationId, SetError.builder()
                    .description("Error when creating the mailbox")
                    .build());
        }
    }

    private MailboxPath getMailboxPath(MailboxRequest mailboxRequest, MailboxSession mailboxSession) throws MailboxException {
        if (mailboxRequest.getParentId().isPresent()) {
            Optional<String> parentName = mailboxUtils.getMailboxNameFromId(mailboxRequest.getParentId().get(), mailboxSession);
            if (parentName.isPresent()) {
                return new MailboxPath(mailboxSession.getPersonalSpace(), mailboxSession.getUser().getUserName(), 
                        parentName.get() + mailboxSession.getPathDelimiter() + mailboxRequest.getName());
            }
            // builder.error parent not found
        }
        return new MailboxPath(mailboxSession.getPersonalSpace(), mailboxSession.getUser().getUserName(), mailboxRequest.getName());
    }
}
