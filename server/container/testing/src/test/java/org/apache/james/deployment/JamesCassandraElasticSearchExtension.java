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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class JamesCassandraElasticSearchExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private Network network = Network.newNetwork();

    private GenericContainer<?> cassandra = new CassandraContainer()
        .withNetwork(network)
        .withNetworkAliases("cassandra");

    private GenericContainer<?> elasticsearch = new GenericContainer<>("elasticsearch:2.4.6")
        .withNetwork(network)
        .withNetworkAliases("elasticsearch");

    private JamesContainer james = new JamesContainer(network);


    @Override
    public void beforeAll(ExtensionContext context) {
        cassandra.start();
        elasticsearch.start();
        james.start();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        james.stop();
        elasticsearch.stop();
        cassandra.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == JamesContainer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return james;
    }
}
