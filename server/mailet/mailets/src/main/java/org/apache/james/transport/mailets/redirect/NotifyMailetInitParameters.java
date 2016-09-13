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

package org.apache.james.transport.mailets.redirect;

import org.apache.mailet.base.GenericMailet;

import com.google.common.base.Strings;

public class NotifyMailetInitParameters implements InitParameters {

    private final GenericMailet mailet;

    public NotifyMailetInitParameters(GenericMailet mailet) {
        this.mailet = mailet;
    }

    @Override
    public boolean getPassThrough() {
        return mailet.getInitParameter("passThrough", true);
    }

    @Override
    public boolean getFakeDomainCheck() {
        return mailet.getInitParameter("fakeDomainCheck", false);
    }

    @Override
    public TypeCode getInLineType() {
        return TypeCode.from(mailet.getInitParameter("inline", "none"));
    }

    @Override
    public TypeCode getAttachmentType() {
        return TypeCode.from(mailet.getInitParameter("attachment", "message"));
    }

    @Override
    public String getMessage() {
        return mailet.getInitParameter("notice", 
                mailet.getInitParameter("message", "We were unable to deliver the attached message because of an error in the mail server."));
    }

    @Override
    public String getSubject() {
        return null;
    }

    @Override
    public String getSubjectPrefix() {
        return mailet.getInitParameter("prefix", "Re:");
    }

    @Override
    public boolean isAttachError() {
        return mailet.getInitParameter("attachError", false);
    }

    @Override
    public boolean isReply() {
        return true;
    }

    @Override
    public String getRecipients() {
        return initParameterOrNull("recipients");
    }

    private String initParameterOrNull(String parameter) {
        String value = mailet.getInitParameter(parameter);
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        return value;
    }

    @Override
    public String getTo() {
        return initParameterOrNull("to");
    }

    @Override
    public String getReversePath() {
        return initParameterOrNull("reversePath");
    }

    @Override
    public String getSender() {
        return initParameterOrNull("sender");
    }

    @Override
    public String getReplyTo() {
        String recipients = mailet.getInitParameter("replyTo", mailet.getInitParameter("replyto"));
        if (Strings.isNullOrEmpty(recipients)) {
            return null;
        }
        return recipients;
    }

    @Override
    public boolean isDebug() {
        return mailet.getInitParameter("debug", false);
    }

    @Override
    public boolean isStatic() {
        return mailet.getInitParameter("static", false);
    }
}
