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

package org.apache.james.util.scanner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.testcontainers.containers.GenericContainer;

import com.github.fge.lambdas.Throwing;

public class SpamAssassinExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private final GenericContainer<?> spamAssassinContainer;
    private SpamAssassin spamAssassin;

    public SpamAssassinExtension() {
        spamAssassinContainer = new GenericContainer<>("aduprat/spamassassin:latest");
        spamAssassinContainer.waitingFor(new SpamAssassinWaitStrategy());
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        spamAssassinContainer.start();
        spamAssassin = new SpamAssassin(spamAssassinContainer);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        spamAssassinContainer.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == SpamAssassin.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return spamAssassin;
    }

    public SpamAssassin getSpamAssassin() {
        return spamAssassin;
    }

    public static class SpamAssassin {
        
        private static final int SPAMASSASSIN_PORT = 783;

        private final String ip;
        private final int bindingPort;
        private final GenericContainer<?> spamAssassinContainer;

        private SpamAssassin(GenericContainer<?> spamAssassinContainer) {
            this.spamAssassinContainer = spamAssassinContainer;
            this.ip = spamAssassinContainer.getContainerIpAddress();
            this.bindingPort = spamAssassinContainer.getMappedPort(SPAMASSASSIN_PORT);
        }

        public String getIp() {
            return ip;
        }
    
        public int getBindingPort() {
            return bindingPort;
        }

        public void train(String user) throws IOException, URISyntaxException {
            train(user, Paths.get(ClassLoader.getSystemResource("spamassassin_db/spam").toURI()), TrainingKind.SPAM);
            train(user, Paths.get(ClassLoader.getSystemResource("spamassassin_db/ham").toURI()), TrainingKind.HAM);
        }

        private void train(String user, Path folder, TrainingKind trainingKind) throws URISyntaxException, IOException {
            spamAssassinContainer.getDockerClient().copyArchiveToContainerCmd(spamAssassinContainer.getContainerId())
                .withHostResource(folder.toAbsolutePath().toString())
                .withRemotePath("/root")
                .exec();
            try (Stream<Path> paths = Files.walk(folder)) {
                paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .forEach(Throwing.consumer(file -> {
                            spamAssassinContainer.execInContainer("sa-learn", 
                                    trainingKind.saLearnExtensionName(), "-u", user,
                                    "/root/" + trainingKind.name().toLowerCase(Locale.US) + "/" +  file.getName());
                    }));
            }
        }

        private static enum TrainingKind {
            SPAM("--spam"), HAM("--ham");

            private String saLearnExtensionName;

            TrainingKind(String saLearnExtensionName) {
                this.saLearnExtensionName = saLearnExtensionName;
            }

            public String saLearnExtensionName() {
                return saLearnExtensionName;
            }
        }

        public void sync(String user) throws UnsupportedOperationException, IOException, InterruptedException {
            spamAssassinContainer.execInContainer("sa-learn", "--sync", "-u", user);
        }

        public void dump(String user) throws UnsupportedOperationException, IOException, InterruptedException {
            spamAssassinContainer.execInContainer("sa-learn", "--dump", "magic", "-u", user);
        }
    }

}
