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

package org.apache.james.mpt.managesieve.cassandra.host;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.init.CassandraModuleComposite;
import org.apache.james.mpt.host.JamesManageSieveHostSystem;
import org.apache.james.sieve.cassandra.CassandraSieveDAO;
import org.apache.james.sieve.cassandra.CassandraSieveRepository;
import org.apache.james.sieve.cassandra.CassandraSieveRepositoryModule;
import org.apache.james.sieverepository.api.SieveRepository;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.cassandra.CassandraUsersRepository;
import org.apache.james.user.cassandra.CassandraUsersRepositoryModule;

public class CassandraHostSystem extends JamesManageSieveHostSystem {
    private static CassandraCluster CASSANDRA_CLUSTER = CassandraCluster.create(new CassandraModuleComposite(
        new CassandraSieveRepositoryModule(),
        new CassandraUsersRepositoryModule()));

    public CassandraHostSystem() throws Exception {
        super(createUsersRepository(), createSieveRepository());
    }

    protected static SieveRepository createSieveRepository() throws Exception {
        return new CassandraSieveRepository(new CassandraSieveDAO(CASSANDRA_CLUSTER.getConf()));
    }

    protected static UsersRepository createUsersRepository() {
        CassandraUsersRepository cassandraUsersRepository = new CassandraUsersRepository();
        cassandraUsersRepository.setSession(CASSANDRA_CLUSTER.getConf());
        cassandraUsersRepository.setEnableVirtualHosting(false);
        return cassandraUsersRepository;
    }

    @Override
    protected void resetData() throws Exception {
        CASSANDRA_CLUSTER.clearAllTables();
    }

    @Override
    public void stop() {
        CASSANDRA_CLUSTER.stop();
    }

}
