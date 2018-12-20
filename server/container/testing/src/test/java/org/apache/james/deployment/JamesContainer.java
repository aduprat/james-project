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

package org.apache.james.deployment;

import static org.apache.james.deployment.DeploymentTest.IMAP_PORT;
import static org.apache.james.deployment.DeploymentTest.JMAP_PORT;
import static org.apache.james.deployment.DeploymentTest.SMTP_PORT;
import static org.apache.james.deployment.DeploymentTest.WEBADMIN_PORT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.MountableFile;

public class JamesContainer extends GenericContainer<JamesContainer> {

    private static final Logger logger = LoggerFactory.getLogger(JamesContainer.class);

    public JamesContainer(Network network) {
        super("linagora/james-project:latest");
        withExposedPorts(JMAP_PORT, SMTP_PORT, IMAP_PORT, WEBADMIN_PORT);
        withCopyFileToContainer(MountableFile.forClasspathResource("/webadmin.properties"), "/root/conf/");
        withCopyFileToContainer(MountableFile.forClasspathResource("/jmap.properties"), "/root/conf/");
        withCopyFileToContainer(MountableFile.forClasspathResource("/keystore"), "/root/conf/");
        withCopyFileToContainer(MountableFile.forClasspathResource("/jwt_publickey"), "/root/conf/");
        withNetwork(network);
        withLogConsumer(log -> logger.info(log.getUtf8String()));
        waitingFor(new HttpWaitStrategy().forPort(WEBADMIN_PORT).forStatusCodeMatching(status -> true));
    }
}
