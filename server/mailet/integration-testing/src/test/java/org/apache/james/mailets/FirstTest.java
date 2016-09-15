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

package org.apache.james.mailets;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FirstTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private FirstTry firstTry;

    @Before
    public void setup() throws Exception {
        DefaultConfigurationBuilder context = new DefaultConfigurationBuilder();
        context.setRootElementName("context");
        context.addProperty("postmaster", "postmaster@james.org");

        DefaultConfigurationBuilder processors = new DefaultConfigurationBuilder();
        processors.setRootElementName("processors");
        processors.addProperty("test", "value");

        firstTry = new FirstTry(temporaryFolder, processors);
    }

    @After
    public void tearDown() {
        firstTry.shutdown();
    }

    @Test
    public void test() {
        
    }
}
