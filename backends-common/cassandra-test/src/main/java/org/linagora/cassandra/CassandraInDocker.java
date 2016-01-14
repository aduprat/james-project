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

package org.linagora.cassandra;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.jayway.awaitility.Awaitility;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;

public class CassandraInDocker {

    private final static Logger LOGGER = LoggerFactory.getLogger(CassandraInDocker.class);

    private final static String IMAGE_NAME = "cassandra";
    private final static String VERSION_SEPARATOR = ":";

    private final static int CASSANDRA_PORT = 9142;
    private final static String EXPOSED_CASSANDRA_PORT = CASSANDRA_PORT + "/tcp";

    private final static HostConfig ALL_PORTS_HOST_CONFIG = HostConfig.builder()
            .publishAllPorts(true)
            .build();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String version;

        private Builder() {
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public CassandraInDocker build() throws DockerException, InterruptedException {
            return new CassandraInDocker(Optional.ofNullable(version));
        }
    }

    private final Optional<String> version;
    private final DockerClient dockerClient;
    private final ContainerCreation container;
    private final int cassandraPort;

    private CassandraInDocker(Optional<String> version) throws DockerException, InterruptedException {
        this.version = version;

        dockerClient = dockerClient();
        container = createContainer();
        start();

        cassandraPort = retrieveCassandraPort(container);
    }

    private DockerClient dockerClient() {
        try {
            DefaultDockerClient dockerClient = DefaultDockerClient.fromEnv().build();
            LOGGER.info("Pulling image");
            dockerClient.pull(imageName());
            return dockerClient;
        } catch (Exception e) {
            LOGGER.error("Fail to pull docker image", e);
            throw Throwables.propagate(e);
        }
    }

    private ContainerCreation createContainer() {
        try {
            LOGGER.info("Creating containre");
            return dockerClient.createContainer(ContainerConfig.builder()
                .image(imageName())
                .networkDisabled(false)
                .exposedPorts(ImmutableSet.of(EXPOSED_CASSANDRA_PORT))
                .build());
        } catch (DockerException | InterruptedException e) {
            LOGGER.error("Fail to create container", e);
            throw Throwables.propagate(e);
        }
    }

    private void start() {
        try {
            dockerClient.startContainer(container.id(), ALL_PORTS_HOST_CONFIG);

            Awaitility
                .await()
                .atMost(30, TimeUnit.SECONDS)
                .with()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .and()
                .ignoreExceptions()
                .until(() -> socketIsReady(container));
        } catch (DockerException | InterruptedException e) {
            LOGGER.error("Fail to start container", e);
            throw Throwables.propagate(e);
        }
    }

    private boolean socketIsReady(ContainerCreation container) throws UnknownHostException, IOException {
        try (Socket socket = new Socket(getHost(), getCassandraPort())) {
            return socket.getInputStream().read() >= 0;
        }
    }

    private String imageName() {
        if (version.isPresent()) {
            return IMAGE_NAME + VERSION_SEPARATOR + version.get();
        }
        return IMAGE_NAME;
    }

    private int retrieveCassandraPort(ContainerCreation container) {
        try {
            return Integer.valueOf(
                    Iterables.getOnlyElement(
                            dockerClient.inspectContainer(
                                    container.id())
                                    .networkSettings()
                                    .ports()
                                    .get(EXPOSED_CASSANDRA_PORT))
                            .hostPort());
        } catch (NumberFormatException | DockerException | InterruptedException e) {
            LOGGER.error("Fail to retrieve Cassandra port", e);
            throw Throwables.propagate(e);
        }
    }

    public String getHost() {
        return dockerClient.getHost();
    }

    public int getCassandraPort() {
        return cassandraPort;
    }

    public void stop() {
        try {
            dockerClient.killContainer(container.id());
            dockerClient.removeContainer(container.id(), true);
        } catch (DockerException | InterruptedException e) {
            LOGGER.error("Fail to stop docker", e);
            Throwables.propagate(e);
        }
    }
}
