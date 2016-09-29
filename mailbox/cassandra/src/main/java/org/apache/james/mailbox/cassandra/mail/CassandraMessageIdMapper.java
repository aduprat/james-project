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
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.ATTACHMENTS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.BODY_START_OCTET;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.FULL_CONTENT_OCTETS;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.HEADER_CONTENT;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.INTERNAL_DATE;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.MOD_SEQ;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.PROPERTIES;
import static org.apache.james.mailbox.cassandra.table.CassandraMessageTable.TEXTUAL_LINE_COUNT;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.mailbox.cassandra.CassandraMessageId;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Attachments;
import org.apache.james.mailbox.cassandra.table.CassandraMessageTable.Properties;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.Attachment;
import org.apache.james.mailbox.model.AttachmentId;
import org.apache.james.mailbox.model.Cid;
import org.apache.james.mailbox.model.MessageAttachment;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.store.mail.AttachmentMapper;
import org.apache.james.mailbox.store.mail.MessageIdMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.SimpleProperty;
import org.apache.james.mailbox.store.mail.model.Message;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.UDTValue;
import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;

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
            UniqueMessageId messageId = retrieveUniqueMessageId(CassandraMessageId.of(row.getString(MESSAGE_ID)));

            SimpleMailboxMessage message =
                    new SimpleMailboxMessage(
                            row.getDate(INTERNAL_DATE),
                            row.getLong(FULL_CONTENT_OCTETS),
                            row.getInt(BODY_START_OCTET),
                            buildContent(row, fetchType),
                            getFlags(row),
                            getPropertyBuilder(row),
                            messageId.getMailboxId(),
                            getAttachments(row, fetchType));
            message.setUid(messageId.getMessageUid());
            message.setMessageId(messageId.getMessageId());
            message.setModSeq(row.getLong(MOD_SEQ));
            return message;
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private UniqueMessageId retrieveUniqueMessageId(CassandraMessageId messageId) throws MailboxException {
        return imapUidDAO.retrieve(messageId, Optional.empty())
            .findFirst()
            .orElseThrow(() -> new MailboxException("Message not found: " + messageId));
    }

    private SharedByteArrayInputStream buildContent(Row row, FetchType fetchType) {
        switch (fetchType) {
            case Full:
                return new SharedByteArrayInputStream(getFullContent(row));
            case Headers:
                return new SharedByteArrayInputStream(getFieldContent(HEADER_CONTENT, row));
            case Body:
                return new SharedByteArrayInputStream(getBodyContent(row));
            case Metadata:
                return new SharedByteArrayInputStream(new byte[]{});
            default:
                throw new RuntimeException("Unknown FetchType " + fetchType);
        }
    }

    private byte[] getFullContent(Row row) {
        return Bytes.concat(getFieldContent(HEADER_CONTENT, row), getFieldContent(BODY_CONTENT, row));
    }

    private byte[] getBodyContent(Row row) {
        return Bytes.concat(new byte[row.getInt(BODY_START_OCTET)], getFieldContent(BODY_CONTENT, row));
    }

    private byte[] getFieldContent(String field, Row row) {
        byte[] headerContent = new byte[row.getBytes(field).remaining()];
        row.getBytes(field).get(headerContent);
        return headerContent;
    }

    private Flags getFlags(Row row) {
        Flags flags = new Flags();
        for (String flag : CassandraMessageTable.Flag.ALL) {
            if (row.getBool(flag)) {
                flags.add(CassandraMessageTable.Flag.JAVAX_MAIL_FLAG.get(flag));
            }
        }
        row.getSet(CassandraMessageTable.Flag.USER_FLAGS, String.class)
            .stream()
            .forEach(flags::add);
        return flags;
    }

    private PropertyBuilder getPropertyBuilder(Row row) {
        PropertyBuilder property = new PropertyBuilder(
            row.getList(PROPERTIES, UDTValue.class).stream()
                .map(x -> new SimpleProperty(x.getString(Properties.NAMESPACE), x.getString(Properties.NAME), x.getString(Properties.VALUE)))
                .collect(Collectors.toList()));
        property.setTextualLineCount(row.getLong(TEXTUAL_LINE_COUNT));
        return property;
    }

    private List<MessageAttachment> getAttachments(Row row, FetchType fetchType) {
        switch (fetchType) {
        case Full:
        case Body:
            List<UDTValue> udtValues = row.getList(ATTACHMENTS, UDTValue.class);
            Map<AttachmentId,Attachment> attachmentsById = attachmentsById(row, udtValues);

            return udtValues
                    .stream()
                    .map(Throwing.function(x -> 
                        MessageAttachment.builder()
                            .attachment(attachmentsById.get(attachmentIdFrom(x)))
                            .name(x.getString(Attachments.NAME))
                            .cid(com.google.common.base.Optional.fromNullable(x.getString(Attachments.CID)).transform(Cid::from))
                            .isInline(x.getBool(Attachments.IS_INLINE))
                            .build()))
                    .collect(Guavate.toImmutableList());
        default:
            return ImmutableList.of();
        }
    }

    private Map<AttachmentId,Attachment> attachmentsById(Row row, List<UDTValue> udtValues) {
        Map<AttachmentId, Attachment> map = new HashMap<>();
        attachmentMapper.getAttachments(attachmentIds(udtValues)).stream()
                .forEach(att -> map.put(att.getAttachmentId(), att));
        return map;
    }

    private List<AttachmentId> attachmentIds(List<UDTValue> udtValues) {
        return udtValues.stream()
            .map(this::attachmentIdFrom)
            .collect(Guavate.toImmutableList());
    }

    private AttachmentId attachmentIdFrom(UDTValue udtValue) {
        return AttachmentId.from(udtValue.getString(Attachments.ID));
    }
}
