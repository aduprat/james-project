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

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.blob.api.Store;
import org.apache.james.blob.mail.MimeMessagePartsId;
import org.apache.james.queue.rabbitmq.MailQueueName;
import org.apache.james.queue.rabbitmq.view.cassandra.model.BucketedSlices.BucketId;
import org.apache.james.queue.rabbitmq.view.cassandra.model.EnqueuedMail;
import org.apache.james.queue.rabbitmq.view.cassandra.model.MailKey;
import org.apache.james.util.CompletableFutureUtil;
import org.apache.mailet.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CassandraMailQueueMailStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraMailQueueMailStore.class);

    private final EnqueuedMailsDAO enqueuedMailsDao;
    private final BrowseStartDAO browseStartDao;
    private final Store<MimeMessage, MimeMessagePartsId> mimeMessageStore;
    private final CassandraMailQueueViewConfiguration configuration;
    private final Clock clock;

    @Inject
    CassandraMailQueueMailStore(EnqueuedMailsDAO enqueuedMailsDao,
                                BrowseStartDAO browseStartDao,
                                Store<MimeMessage, MimeMessagePartsId> mimeMessageStore,
                                CassandraMailQueueViewConfiguration configuration,
                                Clock clock) {
        this.enqueuedMailsDao = enqueuedMailsDao;
        this.browseStartDao = browseStartDao;
        this.mimeMessageStore = mimeMessageStore;
        this.configuration = configuration;
        this.clock = clock;
    }

    CompletableFuture<Void> storeMail(Mail mail, MailQueueName mailQueueName, Instant enqueuedTime) {
        return storeMimeMessage(mail)
            .thenApply(mimePartIds -> convertToEnqueuedMail(mail, mailQueueName, enqueuedTime, mimePartIds))
            .thenCompose(enqueuedMailsDao::insert);
    }

    private CompletableFuture<MimeMessagePartsId> storeMimeMessage(Mail mail) {
        try {
            return mimeMessageStore.save(mail.getMessage());
        } catch (MessagingException e) {
            LOGGER.error("error while getting message for mail {}", mail.getName(), e);
            return CompletableFutureUtil.exceptionallyFuture(e);
        }
    }

    CompletableFuture<Void> initializeBrowseStart(MailQueueName mailQueueName) {
        return browseStartDao
            .insertInitialBrowseStart(mailQueueName, currentSliceStartInstant());
    }

    private EnqueuedMail convertToEnqueuedMail(Mail mail, MailQueueName mailQueueName, Instant enqueuedTime,
                                               MimeMessagePartsId mimeMessagePartsId) {
        return EnqueuedMail.builder()
            .mail(mail)
            .bucketId(computedBucketId(mail))
            .timeRangeStart(currentSliceStartInstant())
            .enqueuedTime(enqueuedTime)
            .mailKey(MailKey.fromMail(mail))
            .mailQueueName(mailQueueName)
            .mimeMessagePartsId(mimeMessagePartsId)
            .build();
    }

    private Instant currentSliceStartInstant() {
        long sliceSize = configuration.getSliceWindow().getSeconds();
        long sliceId = clock.instant().getEpochSecond() / sliceSize;
        return Instant.ofEpochSecond(sliceId * sliceSize);
    }

    private BucketId computedBucketId(Mail mail) {
        int mailKeyHashCode = mail.getName().hashCode();
        int bucketIdValue = mailKeyHashCode % configuration.getBucketCount();
        return BucketId.of(bucketIdValue);
    }
}
