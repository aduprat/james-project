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
package org.apache.james;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.apache.james.modules.protocols.ImapProbe;
import org.apache.james.modules.protocols.LmtpProbe;
import org.apache.james.modules.protocols.Pop3Probe;
import org.apache.james.modules.protocols.SmtpProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

public abstract class AbstractJmapJamesServerTest {

    protected static final String JAMES_SERVER_HOST = "127.0.0.1";

    protected GuiceJamesServer server;
    private SocketChannel socketChannel;

    @Before
    public void setup() throws Exception {
        server = createJamesServer();
        socketChannel = SocketChannel.open();
        server.start();

        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
            .setPort(server.getProbe(JmapGuiceProbe.class).getJmapPort())
            .build();
    }

    protected abstract GuiceJamesServer createJamesServer();

    protected abstract void clean();

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        clean();
    }

    @Test
    public void hostnameShouldBeUsedAsDefaultDomain() throws Exception {
        String expectedDefaultDomain = InetAddress.getLocalHost().getHostName();

        assertThat(server.getProbe(DataProbeImpl.class).getDefaultDomain()).isEqualTo(expectedDefaultDomain);
    }

    @Test
    public void hostnameShouldBeRetrievedWhenRestarting() throws Exception {
        server.stop();
        server.start();
        String expectedDefaultDomain = InetAddress.getLocalHost().getHostName();

        assertThat(server.getProbe(DataProbeImpl.class).getDefaultDomain()).isEqualTo(expectedDefaultDomain);
    }

    @Test
    public void connectIMAPServerShouldSendShabangOnConnect() throws Exception {
        socketChannel.connect(new InetSocketAddress(JAMES_SERVER_HOST, server.getProbe(ImapProbe.class).getPort().get()));
        assertThat(getServerConnectionResponse(socketChannel)).startsWith("* OK JAMES IMAP4rev1 Server");
    }

    @Test
    public void connectOnSecondaryIMAPServerIMAPServerShouldSendShabangOnConnect() throws Exception {
        socketChannel.connect(new InetSocketAddress(JAMES_SERVER_HOST, server.getProbe(ImapProbe.class).getSSLPort().get()));
        assertThat(getServerConnectionResponse(socketChannel)).startsWith("* OK JAMES IMAP4rev1 Server");
    }

    @Test
    public void connectPOP3ServerShouldSendShabangOnConnect() throws Exception {
        socketChannel.connect(new InetSocketAddress(JAMES_SERVER_HOST, server.getProbe(Pop3Probe.class).getPort().get()));
        assertThat(getServerConnectionResponse(socketChannel)).contains("POP3 server (JAMES POP3 Server ) ready");
    }

    @Test
    public void connectSMTPServerShouldSendShabangOnConnect() throws Exception {
        socketChannel.connect(new InetSocketAddress(JAMES_SERVER_HOST, server.getProbe(SmtpProbe.class).getPort().get()));
        assertThat(getServerConnectionResponse(socketChannel)).startsWith("220 JAMES Linagora's SMTP awesome Server");
    }

    @Test
    public void connectLMTPServerShouldSendShabangOnConnect() throws Exception {
        socketChannel.connect(new InetSocketAddress(JAMES_SERVER_HOST, server.getProbe(LmtpProbe.class).getPort().get()));
        assertThat(getServerConnectionResponse(socketChannel)).contains("LMTP Server (JAMES Protocols Server) ready");
    }

    @Test
    public void connectJMAPServerShouldRespondBadRequest() throws Exception {
        given()
            .body("{\"badAttributeName\": \"value\"}")
        .when()
            .post("/authentication")
        .then()
            .statusCode(400);
    }

    private String getServerConnectionResponse(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
        socketChannel.read(byteBuffer);
        byte[] bytes = byteBuffer.array();
        return new String(bytes, Charset.forName("UTF-8"));
    }
}
