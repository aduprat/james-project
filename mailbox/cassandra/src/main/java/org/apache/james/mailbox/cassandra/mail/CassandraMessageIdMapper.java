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

import static org.apache.james.mailbox.cassandra.table.CassandraMessageIds.MESSAGE_ID;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.Message;

import com.datastax.driver.core.Row;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Throwables;

public class CassandraMessageIdMapper implements MessageIdMapper {

    private final AttachmentMapper attachmentMapper;
    private final CassandraImapUidDAO imapUidDAO;
    private final CassandraMessageDAO messageDAO;

    public CassandraMessageIdMapper(AttachmentMapper attachmentMapper, CassandraImapUidDAO imapUidDAO, CassandraMessageDAO messageDAO) {
        this.attachmentMapper = attachmentMapper;
        this.imapUidDAO = imapUidDAO;
        this.messageDAO = messageDAO;
    }

    @Override
    public List<Message> find(List<MessageId> messageIds, FetchType fetchType) {
        List<CassandraMessageId> cassandraMessageIds = messageIds.stream()
            .map(id -> (CassandraMessageId) id)
            .collect(Collectors.toList());
        return CassandraUtils.convertToStream(messageDAO.retrieveMessages(cassandraMessageIds, fetchType, Optional.empty()))
                .map(row -> message(row, fetchType))
                .sorted(Comparator.comparing(MailboxMessage::getUid))
                .collect(Guavate.toImmutableList());
    }

    private MailboxMessage message(Row row, FetchType fetchType) {
        try {
            return CassandraMessageRowHandler.builder()
                        .row(row)
                        .messageId(retrieveUniqueMessageId(CassandraMessageId.of(row.getString(MESSAGE_ID))))
                        .loadingAttachmentsFunction(attachmentMapper::getAttachments)
                        .build()
                    .toMailboxMessage(fetchType);
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private UniqueMessageId retrieveUniqueMessageId(CassandraMessageId messageId) throws MailboxException {
        return imapUidDAO.retrieve(messageId, Optional.empty())
            .findFirst()
            .orElseThrow(() -> new MailboxException("Message not found: " + messageId));
    }
}
