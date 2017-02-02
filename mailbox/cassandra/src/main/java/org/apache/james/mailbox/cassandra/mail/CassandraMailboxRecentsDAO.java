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

import static com.datastax.driver.core.querybuilder.QueryBuilder.bindMarker;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.CassandraId;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxRecentsTable;
import org.apache.james.mailbox.exception.MailboxException;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

public class CassandraMailboxRecentsDAO {

    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final PreparedStatement readStatement;
    private PreparedStatement updateStatement;

    @Inject
    public CassandraMailboxRecentsDAO(Session session) {
        cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        readStatement = createReadStatement(session);
        updateStatement = createUpdateStatement(session);
    }

    private PreparedStatement createReadStatement(Session session) {
        return session.prepare(
            select(CassandraMailboxRecentsTable.RECENT_MESSAGE_UIDS)
                .from(CassandraMailboxRecentsTable.TABLE_NAME)
                .where(eq(CassandraMailboxRecentsTable.MAILBOX_ID, bindMarker(CassandraMailboxRecentsTable.MAILBOX_ID))));
    }

    private PreparedStatement createUpdateStatement(Session session) {
        return session.prepare(
            update(CassandraMailboxRecentsTable.TABLE_NAME)
                .with(set(CassandraMailboxRecentsTable.RECENT_MESSAGE_UIDS, bindMarker(CassandraMailboxRecentsTable.RECENT_MESSAGE_UIDS)))
                .where(eq(CassandraMailboxRecentsTable.MAILBOX_ID, bindMarker(CassandraMailboxRecentsTable.MAILBOX_ID))));
    }

    public CompletableFuture<List<MessageUid>> getRecentMessageUidsInMailbox(CassandraId mailboxId) throws MailboxException {
        return cassandraAsyncExecutor.executeSingleRow(bindWithMailbox(mailboxId, readStatement))
            .thenApply(optional -> optional.map(toMessageUids()).orElse(ImmutableList.<MessageUid> of()));
    }

    private Function<Row, List<MessageUid>> toMessageUids() {
        return row -> row.getList(CassandraMailboxRecentsTable.RECENT_MESSAGE_UIDS, Long.class).stream()
                .map(MessageUid::of)
                .collect(Guavate.toImmutableList());
    }

    private BoundStatement bindWithMailbox(CassandraId mailboxId, PreparedStatement statement) {
        return statement.bind()
            .setUUID(CassandraMailboxRecentsTable.MAILBOX_ID, mailboxId.asUuid());
    }

    public CompletableFuture<Void> removeFromRecent(CassandraId mailboxId, MessageUid messageUid) throws MailboxException {
        List<MessageUid> messageUids = getRecentMessageUidsInMailbox(mailboxId).join();
        List<MessageUid> newMessageUids = messageUids.stream()
                .filter(uid -> !messageUid.equals(uid))
                .collect(Guavate.toImmutableList());
        return cassandraAsyncExecutor.executeVoid(bindWithMailboxAndMessageUids(mailboxId, newMessageUids, updateStatement));
    }

    public CompletableFuture<Void> addToRecent(CassandraId mailboxId, MessageUid messageUid) throws MailboxException {
        List<MessageUid> messageUids = getRecentMessageUidsInMailbox(mailboxId).join();
        List<MessageUid> newMessageUids = ImmutableList.<MessageUid> builder()
                .addAll(messageUids)
                .add(messageUid)
                .build();
        return cassandraAsyncExecutor.executeVoid(bindWithMailboxAndMessageUids(mailboxId, newMessageUids, updateStatement));
    }

    private BoundStatement bindWithMailboxAndMessageUids(CassandraId mailboxId, List<MessageUid> messageUids, PreparedStatement statement) {
        ImmutableList<Long> list = messageUids.stream().map(MessageUid::asLong).collect(Guavate.toImmutableList());
        return statement.bind()
            .setUUID(CassandraMailboxRecentsTable.MAILBOX_ID, mailboxId.asUuid())
            .setList(CassandraMailboxRecentsTable.RECENT_MESSAGE_UIDS, list);
    }
}
