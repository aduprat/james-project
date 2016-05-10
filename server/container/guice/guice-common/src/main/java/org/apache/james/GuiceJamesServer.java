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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.annotation.PreDestroy;

import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.jmap.JMAPServer;
import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.modules.CommonServicesModule;
import org.apache.james.modules.MailetProcessingModule;
import org.apache.james.modules.ProtocolsModule;
import org.apache.james.utils.ConfigurationsPerformer;
import org.apache.james.utils.ExtendedServerProbe;
import org.apache.james.utils.GuiceGenericType;
import org.apache.james.utils.GuiceServerProbe;
import org.apache.onami.lifecycle.core.Stager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

public class GuiceJamesServer<Id extends MailboxId> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiceJamesServer.class);

    private final TypeLiteral<Id> type;
    private final Module module;
    private final GuiceGenericType<Id> guiceGenericType;
    private Stager<PreDestroy> preDestroy;
    private GuiceServerProbe<Id> serverProbe;
    private int jmapPort;

    
    public GuiceJamesServer(TypeLiteral<Id> type) {
        this(type, Modules.combine(
                        new CommonServicesModule<>(type),
                        new ProtocolsModule<>(type),
                        new MailetProcessingModule()));
    }

    private GuiceJamesServer(TypeLiteral<Id> type, Module module) {
        this.type = type;
        this.guiceGenericType = new GuiceGenericType<>(type);
        this.module = module;
    }
    
    public GuiceJamesServer<Id> combineWith(Module... modules) {
        return new GuiceJamesServer<>(type, Modules.combine(Iterables.concat(Arrays.asList(module), Arrays.asList(modules))));
    }
    
    public GuiceJamesServer<Id> overrideWith(Module... overrides) {
        return new GuiceJamesServer<>(type, Modules.override(module).with(overrides));
    }
    
    public void start() throws Exception {
        Injector injector = Guice.createInjector(module);
        injector.getInstance(ConfigurationsPerformer.class).initModules();
        preDestroy = injector.getInstance(Key.get(new TypeLiteral<Stager<PreDestroy>>() {}));
        serverProbe = injector.getInstance(Key.get(guiceGenericType.newGenericType(GuiceServerProbe.class)));
        jmapPort = injector.getInstance(JMAPServer.class).getPort();
        createDefaultDomainIfNone(injector.getInstance(DomainList.class));
    }

    private void createDefaultDomainIfNone(DomainList domainList) {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            if (!domainList.containsDomain(hostName)) {
                domainList.createDefaultDomain(hostName);
            }
        } catch (UnknownHostException e) {
            LOGGER.warn("Unable to retrieve hostname.", e);
        } catch (DomainListException e) {
            LOGGER.error("An error occured while creating the default domain", e);
        }
    }

    public void stop() {
        preDestroy.stage();
    }

    public ExtendedServerProbe<Id> serverProbe() {
        return serverProbe;
    }

    public int getJmapPort() {
        return jmapPort;
    }
}
