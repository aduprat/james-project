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

package org.apache.james.mpt.smtp;

import static org.apache.james.modules.protocols.SmtpGuiceProbe.SmtpServerConnectedType.SMTP_GLOBAL_SERVER;

import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;

public class AwsS3RabbitMQForwardSmtpTest extends ForwardSmtpTest {

    @ClassRule public static DockerCassandraRule cassandraServer = new DockerCassandraRule();

    @Rule
    public SmtpTestRule cassandraRabbitMQAwsS3SmtpTestRule = CassandraRabbitMQAwsS3SmtpTestRuleFactory.create(SMTP_GLOBAL_SERVER, cassandraServer.getHost());

    @Before
    @Override
    public void setUp() throws Exception {
        cassandraRabbitMQAwsS3SmtpTestRule.beforeTest();
        super.setUp();
    }

    @Override
    protected SmtpHostSystem createSmtpHostSystem() {
        return cassandraRabbitMQAwsS3SmtpTestRule;
    }

    @After
    public void tearDown() {
        cassandraRabbitMQAwsS3SmtpTestRule.afterTest();
    }
}