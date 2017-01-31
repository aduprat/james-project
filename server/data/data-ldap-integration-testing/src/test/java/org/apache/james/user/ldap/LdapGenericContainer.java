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
package org.apache.james.user.ldap;

import org.apache.james.util.streams.SwarmGenericContainer;
import org.testcontainers.containers.BindMode;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class LdapGenericContainer {

    public static final int DEFAULT_LDAP_PORT = 389;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String domain;
        private String password;

        private Builder() {
        }

        public Builder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public LdapGenericContainer build() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(domain), "'domain' is mandatory");
            Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "'password' is mandatory");
            return new LdapGenericContainer(createContainer());
        }

        private SwarmGenericContainer createContainer() {
            SwarmGenericContainer container = new SwarmGenericContainer("dinkel/openldap:latest")
                    .withAffinityToContainer();
            container.addEnv("SLAPD_DOMAIN", domain);
            container.addEnv("SLAPD_PASSWORD", password);
            container.addEnv("SLAPD_CONFIG_PASSWORD", password);
            container.withClasspathResourceMapping("ldif-files", "/etc/ldap.dist/prepopulate", BindMode.READ_ONLY);
            container.withExposedPorts(DEFAULT_LDAP_PORT);
            return container;
        }
    }

    private final SwarmGenericContainer container;

    private LdapGenericContainer(SwarmGenericContainer container) {
        this.container = container;
    }

    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    public String getLdapHost() {
        return "ldap://" +
                container.getContainerIpAddress() +
                ":" + 
                container.getMappedPort(LdapGenericContainer.DEFAULT_LDAP_PORT);
    }
}
