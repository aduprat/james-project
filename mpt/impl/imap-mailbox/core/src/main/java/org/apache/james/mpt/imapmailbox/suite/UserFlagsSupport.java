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

import org.apache.james.mpt.api.ImapFeatures;
import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.suite.base.BaseAuthenticatedState;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Test;

import com.google.inject.Inject;

public class UserFlagsSupport extends BaseAuthenticatedState {

    @Inject
    private static ImapHostSystem system;

    public UserFlagsSupport() throws Exception {
        super(system);
    }

    @AfterClass
    public static void stop() {
        system.stop();
    }

    @Test
    public void testUserFlagsSupport() throws Exception {
        Assume.assumeTrue(system.supports(ImapFeatures.Feature.USER_FLAGS_SUPPORT));
        scriptTest("UserFlagsSupport", Locale.US);
    }

}
