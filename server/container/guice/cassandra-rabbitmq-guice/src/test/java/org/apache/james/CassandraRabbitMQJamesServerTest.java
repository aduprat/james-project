/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.james;

import static org.awaitility.Duration.ONE_HUNDRED_MILLISECONDS;

import java.io.IOException;

import org.apache.james.backend.rabbitmq.DockerRabbitMQTestRule;
import org.apache.james.core.Domain;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.modules.protocols.SmtpGuiceProbe;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.james.utils.SpoolerProbe;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class CassandraRabbitMQJamesServerTest extends AbstractJmapJamesServerTest {
    private static final String DOMAIN = "domain";
    private static final String JAMES_USER = "james-user@" + DOMAIN;
    private static final String PASSWORD = "secret";
    private static Duration slowPacedPollInterval = ONE_HUNDRED_MILLISECONDS;
    private static ConditionFactory calmlyAwait = Awaitility.with()
        .pollInterval(slowPacedPollInterval)
        .and()
        .with()
        .pollDelay(slowPacedPollInterval)
        .await();

    private DockerRabbitMQTestRule rabbitMQ = new DockerRabbitMQTestRule();
    private CassandraRabbitMQJmapTestRule cassandraRabbitMQJmap = CassandraRabbitMQJmapTestRule.defaultTestRule();

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(rabbitMQ).around(cassandraRabbitMQJmap);

    @Rule
    public IMAPMessageReader imapMessageReader = new IMAPMessageReader();
    @Rule
    public SMTPMessageSender messageSender = new SMTPMessageSender(Domain.LOCALHOST.asString());

    @Override
    protected GuiceJamesServer createJamesServer() throws IOException {
        return cassandraRabbitMQJmap.jmapServer(rabbitMQ.getDockerRabbitMQ(), DOMAIN_LIST_CONFIGURATION_MODULE);
    }

    @Override
    protected void clean() {

    }

    @Test
    public void mailsShouldBeWellReceived() throws Exception {
        server.getProbe(DataProbeImpl.class).fluent()
            .addDomain(DOMAIN)
            .addUser(JAMES_USER, PASSWORD);

        messageSender.connect(JAMES_SERVER_HOST, server.getProbe(SmtpGuiceProbe.class).getSmtpPort())
            .sendMessage("bob@any.com", JAMES_USER);

        calmlyAwait.until(() -> server.getProbe(SpoolerProbe.class).processingFinished());

        imapMessageReader.connect(JAMES_SERVER_HOST, server.getProbe(ImapGuiceProbe.class).getImapPort())
            .login(JAMES_USER, PASSWORD)
            .select("INBOX")
            .awaitMessage(calmlyAwait);
    }

}
