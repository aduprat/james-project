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
package org.apache.james.backends.cassandra.init;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.backends.cassandra.init.configuration.ClusterConfiguration;
import org.apache.james.util.Host;
import org.junit.jupiter.api.Test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ConsistencyLevel;

class ClusterFactoryTest {

    @Test
    void consistencyLevelShouldBeEqualToQuorum() {
        Cluster cluster = ClusterFactory.create(ClusterConfiguration.builder()
                .host(Host.from("localhost", ClusterConfiguration.DEFAULT_CASSANDRA_PORT))
                .build());

        ConsistencyLevel consistencyLevel = cluster.getConfiguration()
                .getQueryOptions()
                .getConsistencyLevel();

        assertThat(consistencyLevel).isEqualTo(ConsistencyLevel.QUORUM);
    }
}
