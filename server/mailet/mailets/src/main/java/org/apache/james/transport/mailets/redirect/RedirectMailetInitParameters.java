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

import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class RedirectMailetInitParameters implements InitParameters {

    public static InitParameters from(GenericMailet mailet) {
        RedirectMailetInitParameters initParameters = new RedirectMailetInitParameters(mailet, Optional.<TypeCode> absent(), Optional.<TypeCode> absent());
        if (initParameters.isStatic()) {
            return StaticInitParameters.from(initParameters);
        }
        return initParameters;
    }

    public static InitParameters from(GenericMailet mailet, Optional<TypeCode> defaultAttachmentType, Optional<TypeCode> defaultInLineType) {
        RedirectMailetInitParameters initParameters = new RedirectMailetInitParameters(mailet, defaultAttachmentType, defaultAttachmentType);
        if (initParameters.isStatic()) {
            return StaticInitParameters.from(initParameters);
        }
        return initParameters;
    }

    private final GenericMailet mailet;
    private final Optional<TypeCode> defaultAttachmentType;
    private final Optional<TypeCode> defaultInLineType;

    private RedirectMailetInitParameters(GenericMailet mailet, Optional<TypeCode> defaultAttachmentType, Optional<TypeCode> defaultInLineType) {
        this.mailet = mailet;
        this.defaultAttachmentType = defaultAttachmentType;
        this.defaultInLineType = defaultInLineType;
    }

    @Override
    public boolean getPassThrough() {
        return mailet.getInitParameter("passThrough", false);
    }

    @Override
    public boolean getFakeDomainCheck() {
        return mailet.getInitParameter("fakeDomainCheck", false);
    }

    @Override
    public TypeCode getInLineType() {
        return defaultInLineType.or(TypeCode.from(mailet.getInitParameter("inline", "unaltered")));
    }

    @Override
    public TypeCode getAttachmentType() {
        return defaultAttachmentType.or(TypeCode.from(mailet.getInitParameter("attachment", "none")));
    }

    @Override
    public String getMessage() {
        return mailet.getInitParameter("message", "");
    }

    @Override
    public String getSubject() {
        return mailet.getInitParameter("subject");
    }

    @Override
    public String getSubjectPrefix() {
        return mailet.getInitParameter("prefix");
    }

    @Override
    public boolean isAttachError() {
        return mailet.getInitParameter("attachError", false);
    }

    @Override
    public boolean isReply() {
        return mailet.getInitParameter("isReply", false);
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
