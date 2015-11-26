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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.james.jmap.model.FilterCondition;
import org.apache.james.jmap.model.GetMessageListRequest;
import org.apache.james.jmap.model.GetMessageListResponse;
import org.apache.james.jmap.utils.SortToComparatorConvertor;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.store.MailboxSessionMapperFactory;
import org.apache.james.mailbox.store.mail.MessageMapper;
import org.apache.james.mailbox.store.mail.MessageMapper.FetchType;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

public class GetMessageListMethod<Id extends MailboxId> implements Method {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetMailboxesMethod.class);
    private static final Method.Name METHOD_NAME = Method.name("getMessageList");
    private static final int NO_LIMIT = -1;

    private final MailboxManager mailboxManager;
    private final MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory;

    @Inject
    @VisibleForTesting public GetMessageListMethod(MailboxManager mailboxManager, MailboxSessionMapperFactory<Id> mailboxSessionMapperFactory) {
        this.mailboxManager = mailboxManager;
        this.mailboxSessionMapperFactory = mailboxSessionMapperFactory;
    }

    @Override
    public Name methodName() {
        return METHOD_NAME;
    }

    @Override
    public Class<? extends JmapRequest> requestType() {
        return GetMessageListRequest.class;
    }

    @Override
    public GetMessageListResponse process(JmapRequest request, MailboxSession mailboxSession) {
        Preconditions.checkArgument(request instanceof GetMessageListRequest);
        try {
            return getMessageListResponse((GetMessageListRequest) request, mailboxSession);
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private GetMessageListResponse getMessageListResponse(GetMessageListRequest jmapRequest, MailboxSession mailboxSession) throws MailboxException {
        GetMessageListResponse.Builder builder = GetMessageListResponse.builder();

        mailboxManager.list(mailboxSession)
            .stream()
            .filter(mailboxPath -> isMailboxRequested(jmapRequest, mailboxPath, mailboxSession))
            .map(mailboxPath -> getMessages(mailboxPath, mailboxSession))
            .flatMap(List::stream)
            .sorted(comparatorFor(jmapRequest))
            .skip(jmapRequest.getPosition())
            .map(Message::getUid)
            .map(String::valueOf)
            .forEach(builder::messageId);

        return builder.build();
    }

    private Comparator<Message<Id>> comparatorFor(GetMessageListRequest jmapRequest) {
        return SortToComparatorConvertor.comparatorFor(jmapRequest.getSort());
    }

    private boolean isMailboxRequested(GetMessageListRequest jmapRequest, MailboxPath mailboxPath, MailboxSession mailboxSession) {
        if (jmapRequest.getFilter().isPresent()) {
            return jmapRequest.getFilter()
                .filter(FilterCondition.class::isInstance)
                .map(FilterCondition.class::cast)
                .map(FilterCondition::getInMailboxes)
                .filter(list -> isMailboxInList(mailboxPath, mailboxSession, list))
                .isPresent();
        }
        return true;
    }

    private boolean isMailboxInList(MailboxPath mailboxPath, MailboxSession mailboxSession, List<String> inMailboxes) {
        Optional<String> mailboxName = getMailboxName(mailboxPath, mailboxSession);
        if (mailboxName.isPresent()) {
            return inMailboxes.contains(mailboxName.get());
        }
        return true;
    }

    private Optional<String> getMailboxName(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        try {
            return Optional.of(mailboxSessionMapperFactory.getMailboxMapper(mailboxSession)
                    .findMailboxByPath(mailboxPath)
                    .getName());
        } catch (MailboxException e) {
            LOGGER.warn("Error retrieveing mailboxId :" + mailboxPath, e);
            return Optional.empty();
        }
    }

    private Optional<MessageManager> getMessageManager(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        try {
            return Optional.of(mailboxManager.getMailbox(mailboxPath, mailboxSession));
        } catch (MailboxException e) {
            LOGGER.warn("Error retrieveing mailbox :" + mailboxPath, e);
            return Optional.empty();
        }
    }

    private List<Message<Id>> getMessages(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.andCriteria(SearchQuery.all());
        try {
            MessageMapper<Id> messageMapper = mailboxSessionMapperFactory.getMessageMapper(mailboxSession);
            Optional<MessageManager> messageManager = getMessageManager(mailboxPath, mailboxSession);
            return ImmutableList.copyOf(messageManager.get().search(searchQuery, mailboxSession))
                    .stream()
                    .map(messageId -> getMessage(mailboxPath, mailboxSession, messageMapper, messageId))
                    .map(optional -> optional.get())
                    .collect(Collectors.toList());
        } catch (MailboxException e) {
            LOGGER.warn("Error when searching messages for query :" + searchQuery, e);
            return ImmutableList.of();
        }
    }

    private Optional<Message<Id>> getMessage(MailboxPath mailboxPath, MailboxSession mailboxSession, MessageMapper<Id> messageMapper, long messageId) {
        try {
            return ImmutableList.copyOf(messageMapper.findInMailbox(
                        getMailbox(mailboxPath, mailboxSession).get(), 
                        MessageRange.one(messageId),
                        FetchType.Metadata,
                        NO_LIMIT))
                    .stream()
                    .findFirst();
        } catch (MailboxException e) {
            LOGGER.warn("Error retrieveing message :" + messageId, e);
            return Optional.empty();
        }
    }

    private Optional<Mailbox<Id>> getMailbox(MailboxPath mailboxPath, MailboxSession mailboxSession) {
        try {
            return Optional.of(mailboxSessionMapperFactory.getMailboxMapper(mailboxSession)
                    .findMailboxByPath(mailboxPath));
        } catch (MailboxException e) {
            LOGGER.warn("Error retrieveing mailboxId :" + mailboxPath, e);
            return Optional.empty();
        }
    }
}
