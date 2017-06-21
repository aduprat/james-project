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

package org.apache.james.mpt.imapmailbox.suite;

import java.util.Locale;

import javax.inject.Inject;

import org.apache.james.mpt.api.HostSystem;
import org.apache.james.mpt.imapmailbox.ImapTestConstants;
import org.apache.james.mpt.imapmailbox.suite.base.BasicImapCommands;
import org.apache.james.mpt.script.SimpleScriptedTestProtocol;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FetchHeaders implements ImapTestConstants {

    @Inject
    private static HostSystem system;
    
    
    private SimpleScriptedTestProtocol simpleScriptedTestProtocol;

    @Before
    public void setUp() throws Exception {
        simpleScriptedTestProtocol = new SimpleScriptedTestProtocol("/org/apache/james/imap/scripts/", system)
                .withUser(USER, PASSWORD)
                .withLocale(Locale.US);
        BasicImapCommands.welcome(simpleScriptedTestProtocol);
        BasicImapCommands.authenticate(simpleScriptedTestProtocol);
        BasicImapCommands.prepareMailbox(simpleScriptedTestProtocol);
    }
    
    @After
    public void tearDown() throws Exception {
        system.afterTest();
    }

    @Test
    public void testFetchHeaderFieldsUS() throws Exception {
        simpleScriptedTestProtocol
            .withLocale(Locale.US)
            .run("FetchHeaderFields");
    }

    @Test
    public void testFetchHeaderFieldsITALY() throws Exception {
        simpleScriptedTestProtocol
            .withLocale(Locale.ITALY)
            .run("FetchHeaderFields");
    }

    @Test
    public void testFetchHeaderFieldsKOREA() throws Exception {
        simpleScriptedTestProtocol
            .withLocale(Locale.KOREA)
            .run("FetchHeaderFields");
    }

    @Test
    public void testFetchHeaderFieldsNotUS() throws Exception {
        simpleScriptedTestProtocol
            .withLocale(Locale.US)
            .run("FetchHeaderFieldsNot");
    }

    @Test
    public void testFetchHeaderFieldsNotITALY() throws Exception {
        simpleScriptedTestProtocol
            .withLocale(Locale.ITALY)
            .run("FetchHeaderFieldsNot");
    }

    @Test
    public void testFetchHeaderFieldsNotKOREA() throws Exception {
        simpleScriptedTestProtocol
            .withLocale(Locale.KOREA)
            .run("FetchHeaderFieldsNot");
    }
}
