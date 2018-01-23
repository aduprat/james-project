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

package org.apache.james.queue.api;

import static org.assertj.core.api.Assertions.assertThat;

import javax.mail.MessagingException;

import org.junit.jupiter.api.Test;

public interface ManageableMailQueueFactoryContract {

    String NAME_1 = "name1";

    MailQueueFactory<ManageableMailQueue> getMailQueueFactory();

    @Test
    default void createMailQueueShouldNotConflictIfAlreadyExists() throws MessagingException {
        MailQueueFactory<ManageableMailQueue> mailQueueFactory = getMailQueueFactory();
        MailQueue firstCreation = mailQueueFactory.createQueue(NAME_1);

        firstCreation.enQueue(Mails.defaultMail().build());

        ManageableMailQueue secondCreation = mailQueueFactory.createQueue(NAME_1);
        assertThat(secondCreation.getSize()).isEqualTo(1);
    }

}