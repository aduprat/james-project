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
package org.apache.james.mailbox.quota.mailing.search;

import org.apache.james.core.Domain;
import org.apache.james.dnsservice.api.InMemoryDNSService;
import org.apache.james.domainlist.api.DomainListException;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.eventsourcing.eventstore.memory.InMemoryEventStore;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class InMemoryQuotaSearcherExtension implements ParameterResolver {

    private InMemoryEventStore eventStore;
    private MemoryQuotaSearcher quotaSearcher;

    public InMemoryQuotaSearcherExtension() throws DomainListException, UsersRepositoryException {
        MemoryDomainList domainList = new MemoryDomainList(new InMemoryDNSService());
        domainList.setAutoDetect(false);
        domainList.setAutoDetectIP(false);
        domainList.addDomain(Domain.of("bar.com"));
        domainList.addDomain(Domain.of("JoeBarTeam.com"));

        MemoryUsersRepository usersRepository = MemoryUsersRepository.withVirtualHosting();
        usersRepository.setDomainList(domainList);
        usersRepository.addUser("foo@bar.com", "password");
        usersRepository.addUser("foo2@bar.com", "password");
        usersRepository.addUser("foo3@bar.com", "password");
        usersRepository.addUser("foo@JoeBarTeam.com", "password");
        usersRepository.addUser("foo5@bar.com", "password");

        eventStore = new InMemoryEventStore();
        quotaSearcher = new MemoryQuotaSearcher(usersRepository, eventStore);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == QuotaSearcher.class
            || parameterContext.getParameter().getType() == EventStore.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.getParameter().getType().equals(EventStore.class)) {
            return eventStore;
        }
        return quotaSearcher;
    }

}
