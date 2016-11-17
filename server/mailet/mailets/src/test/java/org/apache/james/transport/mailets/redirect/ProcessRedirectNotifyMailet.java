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

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailImpl;
import org.apache.mailet.Mail;
import org.apache.mailet.base.StringUtils;

public class ProcessRedirectNotifyMailet {

    public static ProcessRedirectNotifyMailet from(RedirectNotify mailet) {
        return new ProcessRedirectNotifyMailet(mailet);
    }

    private final RedirectNotify mailet;

    private ProcessRedirectNotifyMailet(RedirectNotify mailet) {
        this.mailet = mailet;
    }

    public void service(Mail originalMail) throws MessagingException {

        boolean keepMessageId = false;

        // duplicates the Mail object, to be able to modify the new mail keeping
        // the original untouched
        MailImpl newMail = new MailImpl(originalMail);
        try {
            MailModifier mailModifier = MailModifier.builder()
                    .mailet(mailet)
                    .mail(newMail)
                    .dns(mailet.getDNSService())
                    .build();
            mailModifier.setRemoteAddr();
            mailModifier.setRemoteHost();

            if (mailet.getInitParameters().isDebug()) {
                mailet.log("New mail - sender: " + newMail.getSender() + ", recipients: " + StringUtils.arrayToString(newMail.getRecipients().toArray()) + ", name: " + newMail.getName() + ", remoteHost: " + newMail.getRemoteHost() + ", remoteAddr: " + newMail.getRemoteAddr() + ", state: " + newMail.getState()
                        + ", lastUpdated: " + newMail.getLastUpdated() + ", errorMessage: " + newMail.getErrorMessage());
            }

            // Create the message
            if (!mailet.getInitParameters().getInLineType().equals(TypeCode.UNALTERED)) {
                if (mailet.getInitParameters().isDebug()) {
                    mailet.log("Alter message");
                }
                newMail.setMessage(new MimeMessage(Session.getDefaultInstance(System.getProperties(), null)));

                // handle the new message if altered
                AlteredMailUtils.builder()
                    .mailet(mailet)
                    .originalMail(originalMail)
                    .build()
                    .buildAlteredMessage(newMail);

            } else {
                // if we need the original, create a copy of this message to
                // redirect
                if (mailet.getInitParameters().getPassThrough()) {
                    newMail.setMessage(new MimeMessage(originalMail.getMessage()) {
                        protected void updateHeaders() throws MessagingException {
                            if (getMessageID() == null)
                                super.updateHeaders();
                            else {
                                modified = false;
                            }
                        }
                    });
                }
                if (mailet.getInitParameters().isDebug()) {
                    mailet.log("Message resent unaltered.");
                }
                keepMessageId = true;
            }

            // Set additional headers

            mailModifier.setRecipients(mailet.getRecipients(originalMail));
            mailModifier.setTo(mailet.getTo(originalMail));
            mailModifier.setSubjectPrefix(originalMail);
            mailModifier.setReplyTo(mailet.getReplyTo(originalMail), originalMail);
            mailModifier.setReversePath(mailet.getReversePath(originalMail), originalMail);
            mailModifier.setIsReply(mailet.getInitParameters().isReply(), originalMail);
            mailModifier.setSender(mailet.getSender(originalMail), originalMail);
            mailModifier.initializeDateIfNotPresent();
            if (keepMessageId) {
                mailModifier.setMessageId(originalMail);
            }
            newMail =  mailModifier.getMail();

            newMail.getMessage().saveChanges();
            newMail.removeAllAttributes();

            if (senderDomainIsValid(newMail)) {
                // Send it off...
                mailet.getMailetContext().sendMail(newMail);
            } else {
                String logBuffer = mailet.getMailetName() + " mailet cannot forward " + originalMail.getName() + ". Invalid sender domain for " + newMail.getSender() + ". Consider using the Resend mailet " + "using a different sender.";
                throw new MessagingException(logBuffer);
            }

        } finally {
            newMail.dispose();
        }

        if (!mailet.getInitParameters().getPassThrough()) {
            originalMail.setState(Mail.GHOST);
        }
    }


    /**
     * <p>
     * Checks if a sender domain of <i>mail</i> is valid.
     * </p>
     * <p>
     * If we do not do this check, and someone uses a redirection mailet in a
     * processor initiated by SenderInFakeDomain, then a fake sender domain will
     * cause an infinite loop (the forwarded e-mail still appears to come from a
     * fake domain).<br>
     * Although this can be viewed as a configuration error, the consequences of
     * such a mis-configuration are severe enough to warrant protecting against
     * the infinite loop.
     * </p>
     * <p>
     * This check can be skipped if {@link #getFakeDomainCheck(Mail)} returns
     * true.
     * </p>
     *
     * @param mail the mail object to check
     * @return true if the if the sender is null or
     *         {@link org.apache.mailet.MailetContext#getMailServers} returns
     *         true for the sender host part
     */
    @SuppressWarnings("deprecation")
    private boolean senderDomainIsValid(Mail mail) throws MessagingException {
        return !mailet.getInitParameters().getFakeDomainCheck()
                || mail.getSender() == null
                || !mailet.getMailetContext().getMailServers(mail.getSender().getDomain()).isEmpty();
    }
}
