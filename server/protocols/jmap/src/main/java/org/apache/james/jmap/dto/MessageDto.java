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

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageDto {

    public String id;
    public String blobId;
    public String threadId;
    public List<String> mailboxIds;
    public String inReplyToMessageId;
    public boolean isUnread;
    public boolean isFlagged;
    public boolean isAnswered;
    public boolean isDraft;
    public boolean hasAttachment;
    public Map<String, String> headers;
    public EmailerDto from;
    public List<EmailerDto> to;
    public List<EmailerDto> cc;
    public List<EmailerDto> bcc;
    public EmailerDto replyTo;
    public String subject;
    public Date date;
    public int size;
    public String preview;
    public String textBody;
    public String htmlBody;
    public List<AttachmentDto> attachments;
    public List<MessageDto> attachedMessages;
}
