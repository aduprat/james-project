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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.commons.net.imap.IMAPClient;
import org.apache.james.mailbox.model.MailboxConstants;
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

public class CommonMailetConfigurationTest {

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
    public void startingJamesWithCommonMailetConfigurationShouldWork() throws Exception {
    }

    @Test
    public void simpleMailShouldBeSent() throws Exception {
        jamesServer.getServerProbe().addDomain("james.org");
        String from = "user@james.org";
        jamesServer.getServerProbe().addUser(from, PASSWORD);
        String recipient = "user2@james.org";
        jamesServer.getServerProbe().addUser(recipient, PASSWORD);
        jamesServer.getServerProbe().createMailbox(MailboxConstants.USER_NAMESPACE, recipient, "INBOX");

        try (SocketChannel socketChannel = SocketChannel.open();) {
            sendMessage(socketChannel, from, recipient);
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> messageHasBeenSent(socketChannel, recipient));
            calmlyAwait.atMost(Duration.ONE_MINUTE).until(() -> userReceivedMessage(recipient));
        }
    }

    private void sendMessage(SocketChannel socketChannel, String from, String recipient) {
        try {
            String message = "ehlo james.org\r\n" +
                    "mail from:<" + from + ">\r\n" +
                    "rcpt to:<" + recipient + ">\r\n" +
                    "data\r\n" +
                    "subject: test\r\n" +
                    "\r\n" +
                    "content\r\n" +
                    ".\r\n";
            socketChannel.connect(new InetSocketAddress(LOCALHOST_IP, SMTP_PORT));
            socketChannel.write(ByteBuffer.wrap(message.getBytes()));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private boolean messageHasBeenSent(SocketChannel socketChannel, String recipient) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2048);
        while (socketChannel.read(byteBuffer) != -1) {
            byteBuffer.flip();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            String response = new String(bytes);
            if (response.contains("250 2.6.0 Message received")) {
                return true;
            }
        }
        return false;
    }

    private boolean userReceivedMessage(String user) {
        try {
            IMAPClient imapClient = new IMAPClient();
            imapClient.connect(LOCALHOST_IP, IMAP_PORT);
            imapClient.login(user, PASSWORD);
            imapClient.select("INBOX");
            imapClient.fetch("1:1", "ALL");
            String replyString = imapClient.getReplyString();
            return replyString.contains("OK FETCH completed");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
