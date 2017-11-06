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
package org.apache.james.mailbox.cassandra.mail;

import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.backends.cassandra.init.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.modules.CassandraAclModule;
import org.apache.james.mailbox.cassandra.table.CassandraACLTable;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxShares;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.google.common.base.Throwables;

public class CassandraACLMapperTest {

    public static final CassandraId MAILBOX_ID = CassandraId.of(UUID.fromString("464765a0-e4e7-11e4-aba4-710c1de3782b"));
    private CassandraACLMapper cassandraACLMapper;
    private CassandraCluster cassandra;
    private ExecutorService executor;

    @ClassRule public static DockerCassandraRule cassandraServer = new DockerCassandraRule();

    @Before
    public void setUp() {
        cassandra = CassandraCluster.create(new CassandraAclModule(), cassandraServer.getIp(), cassandraServer.getBindingPort());
        cassandraACLMapper = new CassandraACLMapper(cassandra.getConf(),
            new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION),
            CassandraConfiguration.DEFAULT_CONFIGURATION);
        executor = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
        cassandra.close();
    }

    @Test
    public void retrieveACLWhenInvalidInBaseShouldReturnEmptyACL() throws Exception {
        cassandra.getConf().execute(
            insertInto(CassandraACLTable.TABLE_NAME)
                .value(CassandraACLTable.ID, MAILBOX_ID.asUuid())
                .value(CassandraACLTable.ACL, "{\"entries\":{\"bob\":invalid}}")
                .value(CassandraACLTable.VERSION, 1));

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join()).isEqualTo(MailboxShares.EMPTY);
    }

    @Test
    public void retrieveACLWhenNoACLStoredShouldReturnEmptyACL() {
        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join()).isEqualTo(MailboxShares.EMPTY);
    }

    @Test
    public void addACLWhenNoneStoredShouldReturnUpdatedACL() throws Exception {
        MailboxShares.EntryKey key = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);

        cassandraACLMapper.updateACL(MAILBOX_ID,
            MailboxShares.command().key(key).rights(rights).asAddition());

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join())
            .isEqualTo(new MailboxShares().union(key, rights));
    }

    @Test
    public void modifyACLWhenStoredShouldReturnUpdatedACL() throws MailboxException {
        MailboxShares.EntryKey keyBob = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);

        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(keyBob).rights(rights).asAddition());
        MailboxShares.EntryKey keyAlice = new MailboxShares.EntryKey("alice", MailboxShares.NameType.user, false);
        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(keyAlice).rights(rights).asAddition());

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join())
            .isEqualTo(new MailboxShares().union(keyBob, rights).union(keyAlice, rights));
    }

    @Test
    public void removeWhenStoredShouldReturnUpdatedACL() throws MailboxException {
        MailboxShares.EntryKey key = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);

        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(key).rights(rights).asAddition());
        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(key).rights(rights).asRemoval());

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join()).isEqualTo(MailboxShares.EMPTY);
    }

    @Test
    public void replaceForSingleKeyWithNullRightsWhenSingleKeyStoredShouldReturnEmptyACL() throws MailboxException {
        MailboxShares.EntryKey key = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);

        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(key).rights(rights).asAddition());
        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(key).noRights().asReplacement());

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join()).isEqualTo(MailboxShares.EMPTY);
    }

    @Test
    public void replaceWhenNotStoredShouldUpdateACLEntry() throws MailboxException {
        MailboxShares.EntryKey key = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);

        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(key).rights(rights).asReplacement());

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join()).isEqualTo(new MailboxShares().union(key, rights));
    }

    @Test
    public void updateInvalidACLShouldBeBasedOnEmptyACL() throws Exception {
        cassandra.getConf().execute(
            insertInto(CassandraACLTable.TABLE_NAME)
                .value(CassandraACLTable.ID, MAILBOX_ID.asUuid())
                .value(CassandraACLTable.ACL, "{\"entries\":{\"bob\":invalid}}")
                .value(CassandraACLTable.VERSION, 1));
        MailboxShares.EntryKey key = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);

        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(key).rights(rights).asAddition());

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join()).isEqualTo(new MailboxShares().union(key, rights));
    }

    @Test
    public void twoConcurrentUpdatesWhenNoACEStoredShouldReturnACEWithTwoEntries() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        MailboxShares.EntryKey keyBob = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);
        MailboxShares.EntryKey keyAlice = new MailboxShares.EntryKey("alice", MailboxShares.NameType.user, false);
        Future<Boolean> future1 = performACLUpdateInExecutor(executor, keyBob, rights, countDownLatch::countDown);
        Future<Boolean> future2 = performACLUpdateInExecutor(executor, keyAlice, rights, countDownLatch::countDown);
        awaitAll(future1, future2);

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join())
            .isEqualTo(new MailboxShares().union(keyBob, rights).union(keyAlice, rights));
    }

    @Test
    public void twoConcurrentUpdatesWhenStoredShouldReturnACEWithTwoEntries() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        MailboxShares.EntryKey keyBenwa = new MailboxShares.EntryKey("benwa", MailboxShares.NameType.user, false);
        MailboxShares.Rfc4314Rights rights = new MailboxShares.Rfc4314Rights(MailboxShares.Right.Read);
        cassandraACLMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(keyBenwa).rights(rights).asAddition());

        MailboxShares.EntryKey keyBob = new MailboxShares.EntryKey("bob", MailboxShares.NameType.user, false);
        MailboxShares.EntryKey keyAlice = new MailboxShares.EntryKey("alice", MailboxShares.NameType.user, false);
        Future<Boolean> future1 = performACLUpdateInExecutor(executor, keyBob, rights, countDownLatch::countDown);
        Future<Boolean> future2 = performACLUpdateInExecutor(executor, keyAlice, rights, countDownLatch::countDown);
        awaitAll(future1, future2);

        assertThat(cassandraACLMapper.getACL(MAILBOX_ID).join())
            .isEqualTo(new MailboxShares().union(keyBob, rights).union(keyAlice, rights).union(keyBenwa, rights));
    }

    private void awaitAll(Future<?>... futures) 
            throws InterruptedException, ExecutionException, TimeoutException {
        for (Future<?> future : futures) {
            future.get(10L, TimeUnit.SECONDS);
        }
    }

    private Future<Boolean> performACLUpdateInExecutor(ExecutorService executor, MailboxShares.EntryKey key, MailboxShares.Rfc4314Rights rights, CassandraACLMapper.CodeInjector runnable) {
        return executor.submit(() -> {
            CassandraACLMapper aclMapper = new CassandraACLMapper(
                cassandra.getConf(),
                new CassandraUserMailboxRightsDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION),
                CassandraConfiguration.DEFAULT_CONFIGURATION,
                runnable);
            try {
                aclMapper.updateACL(MAILBOX_ID, MailboxShares.command().key(key).rights(rights).asAddition());
            } catch (MailboxException exception) {
                throw Throwables.propagate(exception);
            }
            return true;
        });
    }

}
