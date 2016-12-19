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

package org.apache.james.jmap.cassandra;

import org.apache.james.CassandraJmapTestRule;
import org.apache.james.JmapJamesServer;
import org.apache.james.jmap.methods.integration.GetVacationResponseTest;
import org.apache.james.util.date.ZonedDateTimeProvider;
import org.junit.Ignore;
import org.junit.Rule;

@Ignore("needs messageIdManager cassandra impl")
public class CassandraGetVacationResponseTest extends GetVacationResponseTest {

    @Rule 
    public CassandraJmapTestRule rule = new CassandraJmapTestRule();
    
    @Override
    protected JmapJamesServer createJmapServer(ZonedDateTimeProvider zonedDateTimeProvider) {
        return rule.jmapServer()
                .overrideWith(binder -> binder.bind(ZonedDateTimeProvider.class).toInstance(zonedDateTimeProvider));
    }

    @Override
    protected void await() {
        rule.await();
    }
}
