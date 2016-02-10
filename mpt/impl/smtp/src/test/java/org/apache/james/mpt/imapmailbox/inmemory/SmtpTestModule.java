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
package org.apache.james.mpt.imapmailbox.inmemory;

import java.io.IOException;

import org.apache.james.SMTPJamesServerMain;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.mpt.api.Continuation;
import org.apache.james.mpt.api.Session;
import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.model.JamesUser;
import org.apache.mailet.MailAddress;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Scopes;
import com.google.inject.util.Modules;

public class SmtpTestModule extends AbstractModule {

    private final TemporaryFolder folder = new TemporaryFolder();
    private final String rootDirectory;

    public SmtpTestModule() throws IOException {
        folder.create();
        rootDirectory = folder.newFolder().getAbsolutePath();
    }

    @Override
    protected void configure() {
        install(Modules.override(SMTPJamesServerMain.defaultModule)
                    .with(new MyModule(rootDirectory, folder)));
    }

    private static class MyModule extends AbstractModule {
        
        private final String rootDirectory;
        private final TemporaryFolder folder;

        public MyModule(String rootDirectory, TemporaryFolder folder) {
            this.rootDirectory = rootDirectory;
            this.folder = folder;
        }

        @Override
        protected void configure() {
            bind(InMemoryDNSService.class).in(Scopes.SINGLETON);
            bind(DNSService.class).to(InMemoryDNSService.class);
            bind(TemporaryFolder.class).toInstance(folder);
            bind(SmtpHostSystem.class).to(JamesSmtpHostSystem.class);
            bind(JamesDirectoriesProvider.class).toInstance(new MyJamesDirectoriesProvider(rootDirectory));
        }
    }

    private static class MyJamesDirectoriesProvider implements JamesDirectoriesProvider {

        private final String rootDirectory;

        public MyJamesDirectoriesProvider(String rootDirectory) {
            this.rootDirectory = rootDirectory;
        }

        @Override
        public String getAbsoluteDirectory() {
            return "/";
        }

        @Override
        public String getConfDirectory() {
            return ClassLoader.getSystemResource("conf").getPath();
        }

        @Override
        public String getVarDirectory() {
            return rootDirectory + "/var/";
        }

        @Override
        public String getRootDirectory() {
            return rootDirectory;
        }
    }

//    @Provides
//    @Singleton
//    public SmtpHostSystem provideHostSystem(UsersRepository usersRepository, ConfigurationsPerformer configurationsPerformer) throws Exception {
////        configurationsPerformer.initModules();

    private static class JamesSmtpHostSystem implements SmtpHostSystem {

        private final UsersRepository usersRepository;

        @Inject
        private JamesSmtpHostSystem(UsersRepository usersRepository) {
            this.usersRepository = usersRepository;
        }

        @Override
        public boolean addUser(String user, String password) throws Exception {
            usersRepository.addUser(user, password);
            JamesUser jamesUser = (JamesUser) usersRepository.getUserByName(user);
            jamesUser.setForwarding(true);
            jamesUser.setForwardingDestination(new MailAddress("ray@yopmail.com"));
            return true;
        }

        @Override
        public Session newSession(Continuation continuation) throws Exception {
            return null;
        }

        @Override
        public void beforeTests() throws Exception {
        }

        @Override
        public void afterTests() throws Exception {
        }

        @Override
        public void beforeTest() throws Exception {
        }

        @Override
        public void afterTest() throws Exception {
        }
    }

}
