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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxManagerTest;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.manager.ManagerTestResources;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.Test;

public class MemoryMailboxManagerTest extends MailboxManagerTest {
    @Override
    protected MailboxManager provideMailboxManager() throws MailboxException {
        return MemoryMailboxManagerProvider.provideMailboxManager();
    }

    @Test
    public void createMailboxShouldNotThrowWhenMailboxPathBelongsToUser() throws MailboxException {
        MailboxManager mailboxManager = provideMailboxManager();
        MailboxSession mailboxSession = mailboxManager.login(ManagerTestResources.USER, ManagerTestResources.USER_PASS);
        Optional<MailboxId> mailboxId = mailboxManager.createMailbox(MailboxPath.forUser(ManagerTestResources.USER, "mailboxName"), mailboxSession);
        assertThat(mailboxId).isNotEmpty();
    }

    @Test
    public void createMailboxShouldThrowWhenMailboxPathBelongsToAnotherUser() throws MailboxException {
        MailboxManager mailboxManager = provideMailboxManager();
        MailboxSession mailboxSession = mailboxManager.login(ManagerTestResources.USER, ManagerTestResources.USER_PASS);
        Optional<MailboxId> mailboxId = mailboxManager.createMailbox(MailboxPath.forUser(ManagerTestResources.OTHER_USER, "mailboxName"), mailboxSession);
        assertThat(mailboxId).isNotEmpty(); // Oops, no exception thrown and not empty -> mailbox created...
    }
}
