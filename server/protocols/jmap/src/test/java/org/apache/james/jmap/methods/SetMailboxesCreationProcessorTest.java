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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.jmap.model.MailboxCreationId;
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
import org.apache.james.mailbox.store.TestId;
import org.junit.Test;

import java.util.Optional;

public class SetMailboxesCreationProcessorTest {

    @Test
    public void processShouldReturnEmptyCreatedWhenRequestHasEmptyCreate() {
        SetMailboxesCreationProcessor<TestId> sut = new SetMailboxesCreationProcessor<TestId>(null, null);
        SetMailboxesRequest requestWithEmptyCreate = SetMailboxesRequest.builder().build();

        SetMailboxesResponse result = sut.process(requestWithEmptyCreate, buildStubbedSession());

        assertThat(result.getCreated()).isEmpty();
        assertThat(result.getNotCreated()).isEmpty();
    }

    @Test
    public void processShouldReturnNonEmptyCreatedWhenRequestHasNonEmptyCreate() throws MailboxException {
        MailboxManager stubManager = mock(MailboxManager.class);
        MailboxUtils<TestId> mailboxUtils = mock(MailboxUtils.class);
        MailboxSession mailboxSession = buildStubbedSession();
        when(mailboxUtils.mailboxFromMailboxPath(new MailboxPath("#private", "user", "name"), mailboxSession))
            .thenReturn(Optional.of(Mailbox.builder().id("1").name("name").build()));
        SetMailboxesCreationProcessor<TestId> sut = new SetMailboxesCreationProcessor<TestId>(stubManager, mailboxUtils);
        SetMailboxesRequest requestWithEmptyCreate = SetMailboxesRequest.builder()
                .create(MailboxCreationId.of("create"), MailboxRequest.builder().id("id").name("name").build())
                .build();

        SetMailboxesResponse result = sut.process(requestWithEmptyCreate, mailboxSession);

        assertThat(result.getCreated()).isNotEmpty();
        assertThat(result.getNotCreated()).isEmpty();
    }

    private MailboxSession buildStubbedSession() {
        MailboxSession.User stubUser = mock(MailboxSession.User.class);
        when(stubUser.getUserName()).thenReturn("user");
        MailboxSession stubSession = mock(MailboxSession.class);
        when(stubSession.getPathDelimiter()).thenReturn('.');
        when(stubSession.getUser()).thenReturn(stubUser);
        when(stubSession.getPersonalSpace()).thenReturn("#private");
        return stubSession;
    }
}