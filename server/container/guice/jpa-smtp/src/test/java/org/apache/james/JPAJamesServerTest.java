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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import javax.persistence.EntityManagerFactory;

import org.apache.james.backends.jpa.JpaTestCluster;
import org.apache.james.domainlist.jpa.model.JPADomain;
import org.apache.james.modules.TestFilesystemModule;
import org.apache.james.rrt.jpa.model.JPARecipientRewrite;
import org.apache.james.user.jpa.model.JPAUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JPAJamesServerTest {

    private static final int SMTP_PORT = 1025;

    private GuiceJamesServer server;
    private SocketChannel socketChannel;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @After
    public void teardown() {
        server.stop();
    }

    private org.apache.james.GuiceJamesServer createJamesServer() {
        return new GuiceJamesServer()
                .combineWith(JPAJamesServerMain.jpaServerModule, JPAJamesServerMain.protocols)
                .overrideWith(
                        new TestFilesystemModule(temporaryFolder),
                        new TestJPAConfigurationModule(),
                        (binder) -> binder.bind(EntityManagerFactory.class)
                            .toInstance(JpaTestCluster.create(JPAUser.class, JPADomain.class, JPARecipientRewrite.class)
                                    .getEntityManagerFactory()));
    }
    
    @Before
    public void setup() throws Exception {
        server = createJamesServer();
        socketChannel = SocketChannel.open();
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void connectSMTPServerShouldSendShabangOnConnect() throws Exception {
        socketChannel.connect(new InetSocketAddress("127.0.0.1", SMTP_PORT));
        assertThat(getServerConnectionResponse(socketChannel)).startsWith("220 JAMES Linagora's SMTP awesome Server");
    }
    
    private String getServerConnectionResponse(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1000);
        socketChannel.read(byteBuffer);
        byte[] bytes = byteBuffer.array();
        return new String(bytes, Charset.forName("UTF-8"));
    }

}
