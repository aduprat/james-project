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

package org.apache.mailet.base.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.HostAddress;
import org.apache.mailet.LookupException;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;
import org.slf4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@SuppressWarnings("deprecation")
public class FakeMailContext implements MailetContext {

    public static Builder builder() {
        return new Builder();
    }

    public static FakeMailContext defaultContext() {
        return builder().build();
    }

    public static class Builder {

        private Logger logger;
        private Optional<MailAddress> postmaster;

        private Builder() {
            postmaster = Optional.absent();
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder postmaster(MailAddress postmaster) {
            this.postmaster = Optional.of(postmaster);
            return this;
        }

        public FakeMailContext build() {
            return new FakeMailContext(Optional.fromNullable(logger), postmaster.orNull());
        }
    }

    public static class SentMail {

        private final MailAddress sender;
        private final Collection<MailAddress> recipients;
        private final MimeMessage msg;
        private final Map<String, Serializable> attributes;

        public SentMail(MailAddress sender, Collection<MailAddress> recipients, MimeMessage msg, Map<String, Serializable> attributes) {
            this.sender = sender;
            this.recipients = ImmutableList.copyOf(recipients);
            this.msg = msg;
            this.attributes = ImmutableMap.copyOf(attributes);
        }

        public SentMail(MailAddress sender, Collection<MailAddress> recipients, MimeMessage msg) {
            this(sender, recipients, msg, ImmutableMap.<String, Serializable>of());
        }

        public MailAddress getSender() {
            return sender;
        }

        public Collection<MailAddress> getRecipients() {
            return recipients;
        }

        public MimeMessage getMsg() {
            return msg;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SentMail)) {
                return false;
            }

            SentMail sentMail = (SentMail) o;

            return Objects.equal(this.sender, sentMail.sender)
                && Objects.equal(this.recipients, sentMail.recipients)
                && Objects.equal(this.attributes, sentMail.attributes);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(sender, recipients, attributes);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("recipients", recipients)
                .add("sender", sender)
                .add("attributeNames", attributes)
                .toString();
        }
    }

    private final HashMap<String, Object> attributes;
    private final List<SentMail> sentMails;
    private final Optional<Logger> logger;
    private final MailAddress postmaster;

    private FakeMailContext(Optional<Logger> logger, MailAddress postmaster) {
        attributes = new HashMap<String, Object>();
        sentMails = new ArrayList<SentMail>();
        this.logger = logger;
        this.postmaster = postmaster;
    }

    public void bounce(Mail mail, String message) throws MessagingException {
        // trivial implementation
    }

    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        // trivial implementation
    }

    /**
     * @deprecated use the generic dnsLookup method
     */
    public Collection<String> getMailServers(String host) {
        return null;  // trivial implementation
    }

    public MailAddress getPostmaster() {
        return postmaster;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Iterator<String> getAttributeNames() {
        return attributes.keySet().iterator();
    }

    public int getMajorVersion() {
        return 0;  // trivial implementation
    }

    public int getMinorVersion() {
        return 0;  // trivial implementation
    }

    public String getServerInfo() {
        return "Mock Server";
    }

    public boolean isLocalServer(String serverName) {
        return serverName.equals("localhost");  // trivial implementation
    }

    /**
     * @deprecated use {@link #isLocalEmail(MailAddress)} instead 
     */
    public boolean isLocalUser(String userAccount) {
        return false;  // trivial implementation
    }

    public boolean isLocalEmail(MailAddress mailAddress) {
        return false;  // trivial implementation
    }

    /**
     * @deprecated use {@link #log(LogLevel level, String message)}
     */
    public void log(String message) {
        System.out.println(message);
    }

    /**
     * @deprecated use {@link #log(LogLevel level, String message, Throwable t)}
     */
    public void log(String message, Throwable t) {
        System.out.println(message);
        t.printStackTrace(System.out);
    }

    public void removeAttribute(String name) {
        // trivial implementation
    }

    public void sendMail(MimeMessage mimemessage) throws MessagingException {
        sentMails.add(new SentMail(null, new ArrayList<MailAddress>(), mimemessage));
    }

    public void sendMail(MailAddress sender, Collection<MailAddress> recipients, MimeMessage msg) throws MessagingException {
        sentMails.add(new SentMail(sender, recipients, msg));
    }

    public void sendMail(MailAddress sender, Collection<MailAddress> recipients, MimeMessage msg, String state) throws MessagingException {
        sentMails.add(new SentMail(sender, recipients, msg));
    }

    public void sendMail(Mail mail) throws MessagingException {
        sentMails.add(new SentMail(mail.getSender(), mail.getRecipients(), mail.getMessage(), buildAttributesMap(mail)));
    }

    private ImmutableMap<String, Serializable> buildAttributesMap(Mail mail) {
        Map<String, Serializable> result = new HashMap<String, Serializable>();
        List<String> attributesNames = Lists.newArrayList(mail.getAttributeNames());
        for (String attributeName: attributesNames) {
            result.put(attributeName, mail.getAttribute(attributeName));
        }
        return ImmutableMap.copyOf(result);
    }

    public void setAttribute(String name, Serializable object) {
        attributes.put(name,object);
    }

    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage msg) throws MessagingException {
        // trivial implementation
    }

    /**
     * @deprecated use the generic dnsLookup method
     */
    public Iterator<HostAddress> getSMTPHostAddresses(String domainName) {
        return null;  // trivial implementation
    }

    public void setAttribute(String name, Object value) {
        throw new UnsupportedOperationException("MOCKed method");
    }

    public void log(LogLevel level, String message) {
        if (logger.isPresent()) {
            switch (level) {
            case INFO:
                logger.get().info(message);
                break;
            case WARN:
                logger.get().warn(message);
                break;
            case ERROR:
                logger.get().error(message);
                break;
            default:
                logger.get().debug(message);
            }
        } else {
            System.out.println("[" + level + "]" + message);
        }
    }

    public void log(LogLevel level, String message, Throwable t) {
        log(level, message);
        log(level, t.getMessage());
    }

    public List<String> dnsLookup(String name, RecordType type) throws LookupException {
        return null;   // trivial implementation
    }

    public List<SentMail> getSentMails() {
        return sentMails;
    }

    @Override
    public Logger getLogger() {
        return logger.orNull();
    }
}
