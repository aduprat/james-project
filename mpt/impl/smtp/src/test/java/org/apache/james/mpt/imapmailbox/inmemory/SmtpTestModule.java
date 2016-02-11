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
import java.util.Iterator;

import org.apache.james.CassandraJamesServerMain;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.cassandra.CassandraDomainListModule;
import org.apache.james.filesystem.api.JamesDirectoriesProvider;
import org.apache.james.modules.JamesPostConstructModule.PostConstructImpl;
import org.apache.james.mpt.api.SmtpHostSystem;
import org.apache.james.mpt.monitor.SystemLoggingMonitor;
import org.apache.james.mpt.session.ExternalSessionFactory;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.cassandra.CassandraRRTModule;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.cassandra.CassandraUsersRepositoryModule;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;

public class SmtpTestModule extends AbstractModule {

    private final TemporaryFolder folder = new TemporaryFolder();
    private final String rootDirectory;
    private final CassandraCluster cassandraClusterSingleton;

    public SmtpTestModule() throws IOException {
        folder.create();
        rootDirectory = folder.newFolder().getAbsolutePath();
        CassandraModuleComposite cassandraModuleComposite = new CassandraModuleComposite(
                new CassandraDomainListModule(),
                new CassandraUsersRepositoryModule(),
                new CassandraRRTModule());
        cassandraClusterSingleton = CassandraCluster.create(cassandraModuleComposite);
    }

    @Override
    protected void configure() {
        install(Modules.override(CassandraJamesServerMain.defaultModule)
                    .with(new MyModule(rootDirectory, cassandraClusterSingleton.getConf())));
    }

    private static class MyModule extends AbstractModule {
        
        private final String rootDirectory;
        private final com.datastax.driver.core.Session session;

        public MyModule(String rootDirectory, com.datastax.driver.core.Session session) {
            this.rootDirectory = rootDirectory;
            this.session = session;
        }

        @Provides
        public com.datastax.driver.core.Session cassandraSession() {
            return session;
        }

        @Override
        protected void configure() {
            bind(InMemoryDNSService.class).in(Scopes.SINGLETON);
            bind(DNSService.class).to(InMemoryDNSService.class);
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

    @Provides
    @Singleton
    public SmtpHostSystem provideHostSystem(DomainList domainList, UsersRepository usersRepository, RecipientRewriteTable recipientRewriteTable, PostConstructImpl postConstructImpl) throws Exception {
        return new JamesSmtpHostSystem(domainList, usersRepository, recipientRewriteTable, postConstructImpl);
    }

    private static class JamesSmtpHostSystem extends ExternalSessionFactory implements SmtpHostSystem {

        private final DomainList domainList;
        private final UsersRepository usersRepository;
        private final RecipientRewriteTable recipientRewriteTable;
        private final PostConstructImpl postConstructImpl;

        @Inject
        private JamesSmtpHostSystem(DomainList domainList, UsersRepository usersRepository, RecipientRewriteTable recipientRewriteTable, PostConstructImpl postConstructImpl) {
            super("localhost", 1025, new SystemLoggingMonitor(), "220 mydomain.tld smtp");
            this.domainList = domainList;
            this.usersRepository = usersRepository;
            this.recipientRewriteTable = recipientRewriteTable;
            this.postConstructImpl = postConstructImpl;
        }

        @Override
        public boolean addUser(String userAtDomain, String password) throws Exception {
            Preconditions.checkArgument(userAtDomain.contains("@"), "The 'user' should contains the 'domain'");
            Iterator<String> split = Splitter.on("@").split(userAtDomain).iterator();
            String user = split.next();
            String domain = split.next();

            domainList.addDomain(domain);
            usersRepository.addUser(userAtDomain, password);

//            DefaultUser jamesUser = (DefaultUser) usersRepository.getUserByName(userAtDomain);
            recipientRewriteTable.addAddressMapping(user, domain, "ray@yopmail.com");
//            jamesUser.setForwarding(true);
//            jamesUser.setForwardingDestination(new MailAddress("ray@yopmail.com"));
            return true;
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
