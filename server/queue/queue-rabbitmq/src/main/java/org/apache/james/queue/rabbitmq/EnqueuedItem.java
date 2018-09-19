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

package org.apache.james.queue.rabbitmq;

import java.time.Clock;
import java.time.Instant;

import org.apache.james.blob.mail.MimeMessagePartsId;
import org.apache.mailet.Mail;

public class EnqueuedItem {

    interface Builder {

        interface RequireMail {
            RequireEnqueuedTime mail(Mail mail);
        }

        interface RequireEnqueuedTime {
            RequireMimeMessagePartsId enqueuedTime(Clock clock);
        }

        interface RequireMimeMessagePartsId {
            ReadyToBuild mimeMessagePartsId(MimeMessagePartsId partsId);
        }

        class ReadyToBuild {
            private final Mail mail;
            private final Instant enqueuedTime;
            private final MimeMessagePartsId partsId;

            ReadyToBuild(Mail mail, Instant enqueuedTime, MimeMessagePartsId partsId) {
                this.mail = mail;
                this.enqueuedTime = enqueuedTime;
                this.partsId = partsId;
            }

            public EnqueuedItem build() {
                return new EnqueuedItem(mail, enqueuedTime, partsId);
            }
        }
    }

    public static Builder.RequireMail builder() {
        return mail -> clock -> partsId -> new Builder.ReadyToBuild(mail, clock.instant(), partsId);
    }

    private final Mail mail;
    private final Instant enqueuedTime;
    private final MimeMessagePartsId partsId;

    EnqueuedItem(Mail mail, Instant enqueuedTime, MimeMessagePartsId partsId) {
        this.mail = mail;
        this.enqueuedTime = enqueuedTime;
        this.partsId = partsId;
    }

    public Mail getMail() {
        return mail;
    }

    public Instant getEnqueuedTime() {
        return enqueuedTime;
    }

    public MimeMessagePartsId getPartsId() {
        return partsId;
    }
}
