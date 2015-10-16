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

import org.apache.james.jmap.model.Mailbox;

public class MailboxDto {

    public static MailboxDto from(Mailbox mailbox) {
        MailboxDto dto = new MailboxDto();
        dto.id = mailbox.getId();
        dto.name = mailbox.getName();
        dto.parentId = mailbox.getParentId();
        dto.role = mailbox.getRole().name();
        dto.sortOrder = mailbox.getSortOrder();
        dto.mustBeOnlyMailbox = mailbox.isMustBeOnlyMailbox();
        dto.mayReadItems = mailbox.isMayReadItems();
        dto.mayAddItems = mailbox.isMayAddItems();
        dto.mayRemoveItems = mailbox.isMayRemoveItems();
        dto.mayCreateChild = mailbox.isMayCreateChild();
        dto.mayRename = mailbox.isMayRename();
        dto.mayDelete = mailbox.isMayDelete();
        dto.totalMessages = mailbox.getTotalMessages();
        dto.unreadMessages = mailbox.getUnreadMessages();
        dto.totalThreads = mailbox.getTotalThreads();
        dto.unreadThreads = mailbox.getUnreadThreads();
        return dto;
    }

    public String id;
    public String name;
    public String parentId;
    public String role;
    public int sortOrder;
    public boolean mustBeOnlyMailbox;
    public boolean mayReadItems;
    public boolean mayAddItems;
    public boolean mayRemoveItems;
    public boolean mayCreateChild;
    public boolean mayRename;
    public boolean mayDelete;
    public int totalMessages;
    public int unreadMessages;
    public int totalThreads;
    public int unreadThreads;
}
