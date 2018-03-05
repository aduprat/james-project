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

package org.apache.james.transport.matchers;

import java.io.Serializable;
import java.util.Collection;
import java.util.Locale;

import javax.mail.MessagingException;

import org.apache.james.core.MailAddress;
import org.apache.james.util.scanner.SpamAssassinInvoker;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMatcher;

import com.google.common.collect.ImmutableList;

/**
 * <p>
 * Matches mails having a <pre>org.apache.james.spamassassin.status</pre> attribute with a <pre>Yes</pre> value.
 * </p>
 * 
 * As an example, here is a part of a mailet pipeline which can be used in your LocalDelivery processor:
 * <pre>{@code
 * <!-- SpamAssassing mailets pipeline -->
 *     <mailet match="RecipientIsLocal" class="SpamAssassin">
 *         <spamdHost>spamassassin</spamdHost>
 *         <spamdPort>783</spamdPort>
 *     </mailet>
 *     <mailet match="IsMarkedAsSpam" class="ToRecipientFolder">
 *         <folder>Spam</folder>
 *         <consume>true</consume>
 *     </mailet>
 * <!-- End of SpamAssassing mailets pipeline -->
 * }</pre>
 */
public class IsMarkedAsSpam extends GenericMatcher {

    private static final String YES = "yes";

    @Override
    public String getMatcherInfo() {
        return "Has org.apache.james.spamassassin.status attribute with a Yes value Matcher";
    }

    @Override
    public void init() throws MessagingException {
    }

    @Override
    public Collection<MailAddress> match(Mail mail) throws MessagingException {
        Serializable attribute = mail.getAttribute(SpamAssassinInvoker.STATUS_MAIL_ATTRIBUTE_NAME);
        if (isMarkedAsSpam(attribute)) {
            System.out.println("isMarkedAsSpam: true");
            return mail.getRecipients();
        }
        System.out.println("isMarkedAsSpam: false");
        return ImmutableList.of();
    }

    private boolean isMarkedAsSpam(Serializable attribute) {
        return attribute != null &&
                attribute.toString()
                    .toLowerCase(Locale.US)
                    .startsWith(YES);
    }

}
