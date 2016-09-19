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

package org.apache.james.transport.mailets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.Collection;

import javax.mail.MessagingException;

import org.apache.james.dnsservice.api.DNSService;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ForwardTest {

    private static final String MAILET_NAME = "mailetName";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Forward forward;
    private FakeMailContext fakeMailContext;
    private MailAddress postmaster;

    @Before
    public void setUp() throws Exception {
        forward = new Forward();
        DNSService dnsService = mock(DNSService.class);
        forward.setDNSService(dnsService);
        postmaster = new MailAddress("postmaster@james.org");
        fakeMailContext = FakeMailContext.builder()
                .postmaster(postmaster)
                .build();

        when(dnsService.getLocalHost()).thenThrow(new UnknownHostException());
    }

    @Test
    public void getMailetInfoShouldReturnValue() {
        assertThat(forward.getMailetInfo()).isEqualTo("Forward Mailet");
    }

    @Test
    public void initShouldThrowWhenUnkownParameter() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("unknown", "error");
        expectedException.expect(MessagingException.class);

        forward.init(mailetConfig);
    }

    @Test
    public void initShouldNotThrowWhenEveryParameters() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("debug", "true");
        mailetConfig.setProperty("passThrough", "false");
        mailetConfig.setProperty("fakeDomainCheck", "false");
        mailetConfig.setProperty("forwardto", "other@james.org");
        mailetConfig.setProperty("forwardTo", "other@james.org");

        forward.init(mailetConfig);
    }

    @Test
    public void initShouldThrowWhenNoForwardToParameters() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        expectedException.expect(MessagingException.class);

        forward.init(mailetConfig);
    }

    @Test
    public void initShouldThrowWhenUnparsableForwardToAddress() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("forwardTo", "user@james@org");
        expectedException.expect(MessagingException.class);

        forward.init(mailetConfig);
    }

    @Test
    public void initShouldThrowWhenForwardToIsEmpty() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("forwardTo", "");
        expectedException.expect(MessagingException.class);

        forward.init(mailetConfig);
    }

    @Test
    public void getInLineTypeShouldReturnUnaltered() {
        assertThat(forward.getInLineType()).isEqualTo(TypeCode.UNALTERED);
    }

    @Test
    public void getAttachmentTypeShouldReturnNone() {
        assertThat(forward.getAttachmentType()).isEqualTo(TypeCode.NONE);
    }

    @Test
    public void getMessageShouldReturnEmptyString() {
        assertThat(forward.getMessage()).isEmpty();
    }

    @Test
    public void getToShouldReturnNull() throws Exception {
        assertThat(forward.getTo()).isNull();
    }

    @Test
    public void getReplyToShouldReturnNull() throws Exception {
        assertThat(forward.getReplyTo()).isNull();
    }

    @Test
    public void getReversePathShouldReturnNull() throws Exception {
        assertThat(forward.getReversePath()).isNull();
    }

    @Test
    public void getSenderShouldReturnNull() throws Exception {
        assertThat(forward.getSender()).isNull();
    }

    @Test
    public void getSubjectShouldReturnNull() throws Exception {
        assertThat(forward.getSubject()).isNull();
    }

    @Test
    public void getSubjectPrefixShouldReturnNull() throws Exception {
        assertThat(forward.getSubjectPrefix()).isNull();
    }

    @Test
    public void attachErrorShouldReturnFalse() throws Exception {
        assertThat(forward.attachError()).isFalse();
    }

    @Test
    public void isReplyShouldReturnFalse() throws Exception {
        assertThat(forward.isReply()).isFalse();
    }

    @Test
    public void getRecipientsShouldReturnRecipientsWhenForwardtoParameters() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("forwardto", "user@james.org, user2@james.org");
        forward.init(mailetConfig);

        Collection<MailAddress> recipients = forward.getRecipients();
        assertThat(recipients).containsOnly(new MailAddress("user@james.org"), new MailAddress("user2@james.org"));
    }

    @Test
    public void getRecipientsShouldReturnRecipientsWhenForwardToParameters() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("forwardTo", "user@james.org, user2@james.org");
        forward.init(mailetConfig);

        Collection<MailAddress> recipients = forward.getRecipients();
        assertThat(recipients).containsOnly(new MailAddress("user@james.org"), new MailAddress("user2@james.org"));
    }

    @Test
    public void getRecipientsShouldReturnSpecialAddressWhenForwardToIsMatchingOne() throws Exception {
        FakeMailetConfig mailetConfig = new FakeMailetConfig(MAILET_NAME, fakeMailContext);
        mailetConfig.setProperty("forwardTo", "postmaster");
        forward.init(mailetConfig);

        assertThat(forward.getRecipients()).containsOnly(postmaster);
    }
}
