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

package org.apache.james.webadmin.routes;

import static com.jayway.restassured.RestAssured.given;
import static org.apache.james.webadmin.WebAdminServer.NO_CONFIGURATION;

import org.apache.james.dnsservice.api.InMemoryDNSService;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.mailbox.inmemory.quota.InMemoryPerUserMaxQuotaManager;
import org.apache.james.metrics.api.NoopMetricFactory;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.jackson.QuotaModule;
import org.apache.james.webadmin.utils.JsonTransformer;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.jayway.restassured.RestAssured;

public class DomainQuotaRoutesNoVirtualHostingTest {

    private static final String QUOTA_DOMAINS = "/quota/domains";
    private static final String FOUND_COM = "found.com";
    private static final String COUNT = "count";
    private WebAdminServer webAdminServer;

    @Before
    public void setUp() throws Exception {
        InMemoryPerUserMaxQuotaManager maxQuotaManager = new InMemoryPerUserMaxQuotaManager();
        MemoryDomainList memoryDomainList = new MemoryDomainList(new InMemoryDNSService());
        memoryDomainList.setAutoDetect(false);
        memoryDomainList.addDomain(FOUND_COM);
        MemoryDomainList domainList = new MemoryDomainList(new InMemoryDNSService());
        domainList.addDomain(FOUND_COM);
        DomainQuotaService domainQuotaService = new DomainQuotaService(maxQuotaManager);
        QuotaModule quotaModule = new QuotaModule();
        MemoryUsersRepository usersRepository = MemoryUsersRepository.withoutVirtualHosting();
        DomainQuotaRoutes domainQuotaRoutes = new DomainQuotaRoutes(domainList, domainQuotaService, usersRepository, new JsonTransformer(quotaModule), ImmutableSet.of(quotaModule));
        webAdminServer = WebAdminUtils.createWebAdminServer(
            new NoopMetricFactory(),
            domainQuotaRoutes);
        webAdminServer.configure(NO_CONFIGURATION);
        webAdminServer.await();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @After
    public void stop() {
        webAdminServer.destroy();
    }

    @Test
    public void routesShouldNotBeDefinedWhenNoVirtualHosting() {
        given()
            .get(QUOTA_DOMAINS + "/" + FOUND_COM + "/" + COUNT)
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404);
    }

}
