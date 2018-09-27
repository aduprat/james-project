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

package org.apache.james.queue.rabbitmq.view.cassandra;

import java.util.concurrent.CompletableFuture;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.blob.api.Store;
import org.apache.james.blob.mail.MimeMessagePartsId;
import org.apache.james.queue.rabbitmq.EnqueuedItem;
import org.apache.james.queue.rabbitmq.view.cassandra.model.EnqueuedItemWithSlicingContext;
import org.apache.james.queue.rabbitmq.view.cassandra.model.MailReference;
import org.apache.james.server.core.MailImpl;
import org.apache.james.util.FluentFutureStream;
import org.apache.mailet.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MailLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailLoader.class);
    private final EnqueuedMailsDAO enqueuedMailsDAO;
    private final Store<MimeMessage, MimeMessagePartsId> mimeMessageStore;

    MailLoader(EnqueuedMailsDAO enqueuedMailsDAO, Store<MimeMessage, MimeMessagePartsId> mimeMessageStore) {
        this.enqueuedMailsDAO = enqueuedMailsDAO;
        this.mimeMessageStore = mimeMessageStore;
    }

    FluentFutureStream<Mail> load(FluentFutureStream<MailReference> references) {
        return references.map(enqueuedMailsDAO::toMail, FluentFutureStream::unboxFutureOptional)
            .map(this::toMailFuture, FluentFutureStream::unboxFuture);
    }

    private CompletableFuture<Mail> toMailFuture(EnqueuedItemWithSlicingContext enqueuedItemWithSlicingContext) {
        EnqueuedItem enqueuedItem = enqueuedItemWithSlicingContext.getEnqueuedItem();
        return mimeMessageStore.read(enqueuedItem.getPartsId())
            .thenApply(mimeMessage -> loadMimeMessage(enqueuedItem, mimeMessage));
    }

    private Mail loadMimeMessage(EnqueuedItem enqueuedItem, MimeMessage mimeMessage) {
        Mail mail = enqueuedItem.getMail();

        try {
            mail.setMessage(mimeMessage);
        } catch (MessagingException e) {
            LOGGER.error("error while setting mime message to mail {}", mail.getName(), e);
        }

        return new MailImpl();
    }
}
