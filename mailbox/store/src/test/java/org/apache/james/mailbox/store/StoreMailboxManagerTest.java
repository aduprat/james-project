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

package org.apache.james.mailbox.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.james.mailbox.MailboxListener;
import org.apache.james.mailbox.MailboxListener.ListenerType;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.acl.SimpleGroupMembershipResolver;
import org.apache.james.mailbox.acl.UnionMailboxACLResolver;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.event.DefaultDelegatingMailboxListener;
import org.apache.james.mailbox.store.mail.MailboxMapper;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.impl.MessageParser;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailbox;
import org.apache.james.mailbox.store.quota.ListeningCurrentQuotaUpdater;
import org.apache.james.mailbox.store.quota.StoreCurrentQuotaManager;
import org.junit.Before;
import org.junit.Test;

public class StoreMailboxManagerTest {

    private static final String CURRENT_USER = "user";
    private static final MailboxId MAILBOX_ID = TestId.of(123);
    public static final int UID_VALIDITY = 42;
    private StoreMailboxManager storeMailboxManager;
    private MailboxMapper mockedMailboxMapper;
    private MailboxSession mockedMailboxSession;

    @Before
    public void setUp() throws MailboxException {
        MailboxSessionMapperFactory mockedMapperFactory = mock(MailboxSessionMapperFactory.class);
        mockedMailboxSession = new MockMailboxSession(CURRENT_USER);
        mockedMailboxMapper = mock(MailboxMapper.class);
        when(mockedMapperFactory.getMailboxMapper(mockedMailboxSession))
            .thenReturn(mockedMailboxMapper);
        storeMailboxManager = new StoreMailboxManager(mockedMapperFactory, new FakeAuthenticator(), new JVMMailboxPathLocker(), new UnionMailboxACLResolver(), new SimpleGroupMembershipResolver(), new MessageParser());
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithIdenticalMailboxes() {
        MailboxPath path = new MailboxPath("namespace", CURRENT_USER, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path, new SimpleMailbox(path, UID_VALIDITY))).isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithIdenticalMailboxesWithNullUser() {
        MailboxPath path = new MailboxPath("namespace", null, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path, new SimpleMailbox(path, UID_VALIDITY))).isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithIdenticalMailboxesWithNullNamespace() {
        MailboxPath path = new MailboxPath(null, CURRENT_USER, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path,
            new SimpleMailbox(new MailboxPath(null, CURRENT_USER, "name"), UID_VALIDITY))).isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithMailboxWithSameNamespaceAndUserWithNullUser() {
        MailboxPath path = new MailboxPath("namespace", null, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path,
            new SimpleMailbox(new MailboxPath("namespace", null, "name2"), UID_VALIDITY))).isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnTrueWithMailboxWithSameNamespaceAndUser() {
        MailboxPath path = new MailboxPath("namespace", CURRENT_USER, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path,
            new SimpleMailbox(new MailboxPath("namespace", CURRENT_USER, "name2"), UID_VALIDITY))).isTrue();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseWithDifferentNamespace() {
        MailboxPath path = new MailboxPath("namespace", CURRENT_USER, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path,
            new SimpleMailbox(new MailboxPath("namespace2", CURRENT_USER, "name"), UID_VALIDITY))).isFalse();
    }

    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseWithDifferentUser() {
        MailboxPath path = new MailboxPath("namespace", CURRENT_USER, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path,
            new SimpleMailbox(new MailboxPath("namespace", "user2", "name"), UID_VALIDITY))).isFalse();
    }
    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseWithOneOfTheUserNull() {
        MailboxPath path = new MailboxPath("namespace", null, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path,
            new SimpleMailbox(new MailboxPath("namespace", CURRENT_USER, "name"), UID_VALIDITY))).isFalse();
    }
    @Test
    public void belongsToNamespaceAndUserShouldReturnFalseIfNamespaceAreDifferentWithNullUser() {
        MailboxPath path = new MailboxPath("namespace", null, "name");
        assertThat(storeMailboxManager.belongsToNamespaceAndUser(path,
            new SimpleMailbox(new MailboxPath("namespace2", null, "name"), UID_VALIDITY))).isFalse();
    }

    @Test(expected = MailboxNotFoundException.class)
    public void getMailboxShouldThrowWhenUnknownId() throws Exception {
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(null);

        storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession);
    }

    @Test
    public void getMailboxShouldReturnMailboxManagerWhenKnownId() throws Exception {
        Mailbox mockedMailbox = mock(Mailbox.class);
        when(mockedMailbox.getUser()).thenReturn(CURRENT_USER);
        when(mockedMailbox.getMailboxId()).thenReturn(MAILBOX_ID);
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(mockedMailbox);

        MessageManager expected = storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession);

        assertThat(expected.getId()).isEqualTo(MAILBOX_ID);
    }

    @Test
    public void getMailboxShouldReturnMailboxManagerWhenKnownIdAndDifferentCaseUser() throws Exception {
        Mailbox mockedMailbox = mock(Mailbox.class);
        when(mockedMailbox.getUser()).thenReturn("uSEr");
        when(mockedMailbox.getMailboxId()).thenReturn(MAILBOX_ID);
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(mockedMailbox);

        MessageManager expected = storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession);

        assertThat(expected.getId()).isEqualTo(MAILBOX_ID);
    }

    @Test(expected = MailboxNotFoundException.class)
    public void getMailboxShouldThrowWhenMailboxDoesNotMatchUser() throws Exception {
        Mailbox mockedMailbox = mock(Mailbox.class);
        when(mockedMailbox.getUser()).thenReturn("other.user");
        when(mockedMailbox.getMailboxId()).thenReturn(MAILBOX_ID);
        when(mockedMailboxMapper.findMailboxById(MAILBOX_ID)).thenReturn(mockedMailbox);

        MessageManager expected = storeMailboxManager.getMailbox(MAILBOX_ID, mockedMailboxSession);

        assertThat(expected.getId()).isEqualTo(MAILBOX_ID);
    }

    @Test
    public void quotaUpdaterShouldBeInitializeOnlyOneTime() throws Exception {
        MyDelegatingMailboxListener delegatingListener = new MyDelegatingMailboxListener();
        storeMailboxManager.setDelegatingMailboxListener(delegatingListener);

        StoreCurrentQuotaManager storeCurrentQuotaManager = mock(StoreCurrentQuotaManager.class);
        when(storeCurrentQuotaManager.getAssociatedListenerType())
            .thenReturn(ListenerType.EACH_NODE);
        ListeningCurrentQuotaUpdater quotaUpdater = new ListeningCurrentQuotaUpdater();
        quotaUpdater.setCurrentQuotaManager(storeCurrentQuotaManager);
        storeMailboxManager.setQuotaUpdater(quotaUpdater);

        storeMailboxManager.init();
        int expectedListenersNumber = delegatingListener.getListeners().size();

        storeMailboxManager.init();

        assertThat(delegatingListener.getListeners().size()).isEqualTo(expectedListenersNumber);
    }

    private static class MyDelegatingMailboxListener extends DefaultDelegatingMailboxListener {

        public List<MailboxListener> getListeners() {
            return getMailboxListenerRegistry().getGlobalListeners();
        }
    }
}

