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

package org.apache.james.webadmin.dto;

import java.util.Collection;
import java.util.List;

import org.apache.james.core.MailAddress;
import org.apache.james.queue.api.MailQueue.MailQueueException;
import org.apache.james.queue.api.ManageableMailQueue;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class MailQueueItemDTO {

    public static Builder builder() {
        return new Builder();
    }

    public static MailQueueItemDTO from(ManageableMailQueue.MailQueueItemView mailQueueItemView) throws MailQueueException {
        return builder()
                .name(mailQueueItemView.getMail().getName())
                .sender(mailQueueItemView.getMail().getSender().asPrettyString())
                .recipients(mailQueueItemView.getMail().getRecipients())
                .delayed(isDelayed(mailQueueItemView))
                .build();
    }

    private static boolean isDelayed(ManageableMailQueue.MailQueueItemView mailQueueItemView) {
        return mailQueueItemView.getNextDelivery() != -1;
    }

    public static class Builder {

        private String name;
        private String sender;
        private List<String> recipients;
        private boolean delayed;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder sender(String snder) {
            this.sender = snder;
            return this;
        }

        public Builder recipients(Collection<MailAddress> recipients) {
            this.recipients = recipients.stream()
                    .map(MailAddress::asPrettyString)
                    .collect(Guavate.toImmutableList());
            return this;
        }

        public Builder delayed(boolean delayed) {
            this.delayed = delayed;
            return this;
        }

        public MailQueueItemDTO build() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "name is mandatory");
            return new MailQueueItemDTO(name, sender, recipients, delayed);
        }
    }

    private final String name;
    private final String sender;
    private final List<String> recipients;
    private final boolean delayed;

    public MailQueueItemDTO(String name, String sender, List<String> recipients, boolean delayed) {
        this.name = name;
        this.sender = sender;
        this.recipients = recipients;
        this.delayed = delayed;
    }

    public String getName() {
        return name;
    }

    public String getSender() {
        return sender;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public boolean isDelayed() {
        return delayed;
    }
}
