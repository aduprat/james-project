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

package org.apache.james.jmap.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.jmap.model.Mailbox;
import org.apache.james.jmap.model.Role;
import org.junit.Test;

public class MailboxDtoTest {

    @Test
    public void fromShouldWork() {
        Mailbox mailbox = Mailbox.builder()
                .id("id")
                .name("name")
                .parentId("parentId")
                .role(Role.DRAFTS)
                .sortOrder(123)
                .mustBeOnlyMailbox(true)
                .mayReadItems(true)
                .mayAddItems(true)
                .mayRemoveItems(true)
                .mayCreateChild(true)
                .mayRename(true)
                .mayDelete(true)
                .totalMessages(456)
                .unreadMessages(789)
                .totalThreads(741)
                .unreadThreads(852)
                .build();

        MailboxDto dto = MailboxDto.from(mailbox);

        assertThat(dto.id).isEqualTo(mailbox.getId());
        assertThat(dto.name).isEqualTo(mailbox.getName());
        assertThat(dto.parentId).isEqualTo(mailbox.getParentId());
        assertThat(dto.role).isEqualTo(mailbox.getRole().name());
        assertThat(dto.sortOrder).isEqualTo(mailbox.getSortOrder());
        assertThat(dto.mustBeOnlyMailbox).isEqualTo(mailbox.isMustBeOnlyMailbox());
        assertThat(dto.mayReadItems).isEqualTo(mailbox.isMayReadItems());
        assertThat(dto.mayAddItems).isEqualTo(mailbox.isMayAddItems());
        assertThat(dto.mayRemoveItems).isEqualTo(mailbox.isMayRemoveItems());
        assertThat(dto.mayCreateChild).isEqualTo(mailbox.isMayCreateChild());
        assertThat(dto.mayRename).isEqualTo(mailbox.isMayRename());
        assertThat(dto.mayDelete).isEqualTo(mailbox.isMayDelete());
        assertThat(dto.totalMessages).isEqualTo(mailbox.getTotalMessages());
        assertThat(dto.unreadMessages).isEqualTo(mailbox.getUnreadMessages());
        assertThat(dto.totalThreads).isEqualTo(mailbox.getTotalThreads());
        assertThat(dto.unreadThreads).isEqualTo(mailbox.getUnreadThreads());
    }
}
