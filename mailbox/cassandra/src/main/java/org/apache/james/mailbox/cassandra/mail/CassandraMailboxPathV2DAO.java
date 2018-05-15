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
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.apache.james.mailbox.cassandra.GhostMailbox.TYPE;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxPathV2Table.FIELDS;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxPathV2Table.MAILBOX_ID;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxPathV2Table.MAILBOX_NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxPathV2Table.NAMESPACE;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxPathV2Table.TABLE_NAME;
import static org.apache.james.mailbox.cassandra.table.CassandraMailboxPathV2Table.USER;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.GhostMailbox;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.model.MailboxPath;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.base.Strings;

public class CassandraMailboxPathV2DAO implements CassandraMailboxPathDAO {


    private final CassandraAsyncExecutor cassandraAsyncExecutor;
    private final CassandraUtils cassandraUtils;
    private final PreparedStatement delete;
    private final PreparedStatement insert;
    private final PreparedStatement select;
    private final PreparedStatement selectAll;

    @Inject
    public CassandraMailboxPathV2DAO(Session session, CassandraUtils cassandraUtils) {
        this.cassandraAsyncExecutor = new CassandraAsyncExecutor(session);
        this.cassandraUtils = cassandraUtils;
        this.insert = prepareInsert(session);
        this.delete = prepareDelete(session);
        this.select = prepareSelect(session);
        this.selectAll = prepareSelectAll(session);
    }

    private PreparedStatement prepareDelete(Session session) {
        return session.prepare(QueryBuilder.delete()
            .from(TABLE_NAME)
            .where(eq(NAMESPACE, bindMarker(NAMESPACE)))
            .and(eq(USER, bindMarker(USER)))
            .and(eq(MAILBOX_NAME, bindMarker(MAILBOX_NAME))));
    }

    private PreparedStatement prepareInsert(Session session) {
        return session.prepare(insertInto(TABLE_NAME)
            .value(NAMESPACE, bindMarker(NAMESPACE))
            .value(USER, bindMarker(USER))
            .value(MAILBOX_NAME, bindMarker(MAILBOX_NAME))
            .value(MAILBOX_ID, bindMarker(MAILBOX_ID))
            .ifNotExists());
    }

    private PreparedStatement prepareSelect(Session session) {
        return session.prepare(select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(NAMESPACE, bindMarker(NAMESPACE)))
            .and(eq(USER, bindMarker(USER)))
            .and(eq(MAILBOX_NAME, bindMarker(MAILBOX_NAME))));
    }

    private PreparedStatement prepareSelectAll(Session session) {
        return session.prepare(select(FIELDS)
            .from(TABLE_NAME)
            .where(eq(NAMESPACE, bindMarker(NAMESPACE)))
            .and(eq(USER, bindMarker(USER))));
    }

    @Override
    public CompletableFuture<Optional<CassandraIdAndPath>> retrieveId(MailboxPath mailboxPath) {
        return cassandraAsyncExecutor.executeSingleRow(
            select.bind()
                .setString(NAMESPACE, mailboxPath.getNamespace())
                .setString(USER, user(mailboxPath))
                .setString(MAILBOX_NAME, mailboxPath.getName()))
            .thenApply(rowOptional ->
                rowOptional.map(this::fromRowToCassandraIdAndPath))
            .thenApply(value -> logGhostMailbox(mailboxPath, value));
    }

    @Override
    public CompletableFuture<Stream<CassandraIdAndPath>> listUserMailboxes(String namespace, String user) {
        return cassandraAsyncExecutor.execute(
            selectAll.bind()
                .setString(NAMESPACE, namespace)
                .setString(USER, user))
            .thenApply(resultSet -> cassandraUtils.convertToStream(resultSet)
                .map(this::fromRowToCassandraIdAndPath)
                .peek(this::logReadSuccess));
    }

    /**
     * See https://issues.apache.org/jira/browse/MAILBOX-322 to read about the Ghost mailbox bug.
     *
     * A missed read on an existing mailbox is the cause of the ghost mailbox bug. Here we log missing reads. Successful
     * reads and write operations are also added in order to allow audit in order to know if the mailbox existed.
     */
    @Override
    public Optional<CassandraIdAndPath> logGhostMailbox(MailboxPath mailboxPath, Optional<CassandraIdAndPath> value) {
        if (value.isPresent()) {
            CassandraIdAndPath cassandraIdAndPath = value.get();
            logReadSuccess(cassandraIdAndPath);
        } else {
            GhostMailbox.logger()
                .addField(GhostMailbox.MAILBOX_NAME, mailboxPath)
                .addField(TYPE, "readMiss")
                .log(logger -> logger.info("Read mailbox missed"));
        }
        return value;
    }

    /**
     * See https://issues.apache.org/jira/browse/MAILBOX-322 to read about the Ghost mailbox bug.
     *
     * Read success allows to know if a mailbox existed before (mailbox write history might be older than this log introduction
     * or log history might have been dropped)
     */
    private void logReadSuccess(CassandraIdAndPath cassandraIdAndPath) {
        GhostMailbox.logger()
            .addField(GhostMailbox.MAILBOX_NAME, cassandraIdAndPath.getMailboxPath())
            .addField(TYPE, "readSuccess")
            .addField(GhostMailbox.MAILBOX_ID, cassandraIdAndPath.getCassandraId())
            .log(logger -> logger.info("Read mailbox succeeded"));
    }

    private CassandraIdAndPath fromRowToCassandraIdAndPath(Row row) {
        return new CassandraIdAndPath(
            CassandraId.of(row.getUUID(MAILBOX_ID)),
            new MailboxPath(row.getString(NAMESPACE),
                row.getString(USER),
                row.getString(MAILBOX_NAME)));
    }

    @Override
    public CompletableFuture<Boolean> save(MailboxPath mailboxPath, CassandraId mailboxId) {
        return cassandraAsyncExecutor.executeReturnApplied(insert.bind()
            .setString(NAMESPACE, mailboxPath.getNamespace())
            .setString(USER, user(mailboxPath))
            .setString(MAILBOX_NAME, mailboxPath.getName())
            .setUUID(MAILBOX_ID, mailboxId.asUuid()));
    }

    @Override
    public CompletableFuture<Void> delete(MailboxPath mailboxPath) {
        return cassandraAsyncExecutor.executeVoid(delete.bind()
            .setString(NAMESPACE, mailboxPath.getNamespace())
            .setString(USER, user(mailboxPath))
            .setString(MAILBOX_NAME, mailboxPath.getName()));
    }

    private String user(MailboxPath mailboxPath) {
        String user = mailboxPath.getUser();
        if (Strings.isNullOrEmpty(user)) {
            return "";
        }
        return user;
    }
}
