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
package org.apache.james.jmap.cassandra.cucumber;

import com.google.inject.Module;
import org.apache.james.GuiceModuleTestRule;
import org.apache.james.backend.rabbitmq.DockerRabbitMQ;
import org.apache.james.backend.rabbitmq.DockerRabbitMQTestRule;
import org.elasticsearch.common.settings.Settings;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class DockerRabbitMQResource extends ExternalResource {

    private final DockerRabbitMQ rabbitMQ;

    public DockerRabbitMQResource() {
        rabbitMQ = DockerRabbitMQ.withoutCookie();
    }

    @Override
    public void before() {
        rabbitMQ.start();
    }

    @Override
    public void after() {
        rabbitMQ.stop();
    }

    public DockerRabbitMQ getRabbitMQ() {
        return rabbitMQ;
    }
}
