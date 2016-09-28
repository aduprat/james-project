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

package org.apache.james.mailets;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.apache.commons.net.imap.IMAPClient;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.james.mailets.configuration.CommonProcessors;
import org.apache.james.mailets.configuration.MailetContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Throwables;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;

public class RRTMailetTest {

    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final int IMAP_PORT = 1143;
    private static final int SMTP_PORT = 1025;
    private static final String PASSWORD = "secret";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TemporaryJamesServer jamesServer;
    private ConditionFactory calmlyAwait;

    @Before
    public void setup() throws Exception {
        MailetContainer mailetContainer = MailetContainer.builder()
            .postmaster("postmaster@james.org")
            .threads(20)
            .addProcessor(CommonProcessors.root())
            .addProcessor(CommonProcessors.error())
            .addProcessor(CommonProcessors.transport())
            .addProcessor(CommonProcessors.spam())
            .addProcessor(CommonProcessors.localAddressError())
            .addProcessor(CommonProcessors.relayDenied())
            .addProcessor(CommonProcessors.bounces())
            .addProcessor(CommonProcessors.sieveManagerCheck())
            .build();

        jamesServer = new TemporaryJamesServer(temporaryFolder, mailetContainer);
        Duration slowPacedPollInterval = Duration.FIVE_HUNDRED_MILLISECONDS;
        calmlyAwait = Awaitility.with().pollInterval(slowPacedPollInterval).and().with().pollDelay(slowPacedPollInterval).await();
    }

    @After
    public void tearDown() {
        jamesServer.shutdown();
    }

    @Test
    public void messagesShouldNotBeSentToAnUnknownUserWhenRRTDomainMatches() throws Exception {
        String domain1 = "james.org";
        String domain2 = "alias.org";
        jamesServer.getServerProbe().addDomain(domain1);
        jamesServer.getServerProbe().addDomain(domain2);

        String realUser = "user@james.org";
        jamesServer.getServerProbe().addUser(realUser, PASSWORD);

        String aliasUser = "user@alias.org";

        String from = "sender@james.org";
        jamesServer.getServerProbe().addUser(from, PASSWORD);

        jamesServer.getServerProbe().addAliasDomainMapping(domain2, domain1);

        SMTPClient smtpClient = new SMTPClient();
        try (SocketChannel socketChannel = SocketChannel.open()) {
            smtpClient.connect(LOCALHOST_IP, SMTP_PORT);
            sendMessage(smtpClient, from, aliasUser);

            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> messageHasBeenSent(smtpClient));
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> userReceivedMessage(realUser));
        } finally {
            smtpClient.disconnect();
        }
    }

    private void sendMessage(SMTPClient smtpClient, String from, String recipient) {
        try {
            System.out.println("sending the email");
            smtpClient.helo("james.org");
            smtpClient.setSender(from);
            smtpClient.rcpt("<" + recipient + ">");
            smtpClient.sendShortMessageData("subject: test\r\n" +
                    "\r\n" +
                    "content\r\n" +
                    ".\r\n");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean messageHasBeenSent(SMTPClient smtpClient) throws IOException {
        System.out.println("messageHasBeenSent");
        return smtpClient.getReplyString()
            .contains("250 2.6.0 Message received");
    }

    private boolean userReceivedMessage(String user) {
        System.out.println("userReceivedMessage");
        IMAPClient imapClient = new IMAPClient();
        try {
            imapClient.connect(LOCALHOST_IP, IMAP_PORT);
            imapClient.login(user, PASSWORD);
            imapClient.select("INBOX");
            imapClient.fetch("1:1", "ALL");
            String replyString = imapClient.getReplyString();
            return replyString.contains("OK FETCH completed");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            try {
                imapClient.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
