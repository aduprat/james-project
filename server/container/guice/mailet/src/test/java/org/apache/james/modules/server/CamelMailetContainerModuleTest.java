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
package org.apache.james.modules.server;

import static org.apache.james.modules.server.CamelMailetContainerModule.MailetModuleConfigurationPerformer.ONE_MINUTE_IN_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.mailetcontainer.impl.JamesMailSpooler;
import org.apache.james.mailetcontainer.impl.JamesMailetContext;
import org.apache.james.mailetcontainer.impl.camel.CamelCompositeProcessor;
import org.apache.james.modules.server.CamelMailetContainerModule.DefaultProcessorsConfigurationSupplier;
import org.apache.james.modules.server.CamelMailetContainerModule.TransportProcessorCheck;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.utils.ConfigurationProvider;
import org.junit.Test;

public class CamelMailetContainerModuleTest {

    private CamelCompositeProcessor camelCompositeProcessor = null;
    private JamesMailSpooler jamesMailSpooler = null;
    private JamesMailetContext mailetContext = null;
    private MailQueueFactory mailQueueFactory = null;
    private Set<TransportProcessorCheck> transportProcessorCheckSet = null;
    private DefaultProcessorsConfigurationSupplier defaultProcessorsConfigurationSupplier = null;
    
    @Test
    public void getInitializationTimeoutShouldReturnDefaultValueWhenExceptionOccured() throws Exception {
        ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
        when(configurationProvider.getConfiguration("mailetcontainer"))
            .thenThrow(new ConfigurationException());
        
        CamelMailetContainerModule.MailetModuleConfigurationPerformer configurationPerformer = new CamelMailetContainerModule.MailetModuleConfigurationPerformer(configurationProvider, 
                camelCompositeProcessor, 
                jamesMailSpooler, 
                mailetContext, 
                mailQueueFactory, 
                transportProcessorCheckSet, 
                defaultProcessorsConfigurationSupplier);
        
        assertThat(configurationPerformer.getInitializationTimeout()).isEqualTo(Duration.ofMillis(ONE_MINUTE_IN_MILLIS));
    }
    
    @Test
    public void getInitializationTimeoutShouldReturnDefaultValueWhenNone() throws Exception {
        HierarchicalConfiguration configuration = mock(HierarchicalConfiguration.class);
        ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
        when(configurationProvider.getConfiguration("mailetcontainer"))
            .thenReturn(configuration);
        
        when(configuration.getLong("initTimeoutInMillis", ONE_MINUTE_IN_MILLIS))
            .thenReturn(ONE_MINUTE_IN_MILLIS);
        
        CamelMailetContainerModule.MailetModuleConfigurationPerformer configurationPerformer = new CamelMailetContainerModule.MailetModuleConfigurationPerformer(configurationProvider, 
                camelCompositeProcessor, 
                jamesMailSpooler, 
                mailetContext, 
                mailQueueFactory, 
                transportProcessorCheckSet, 
                defaultProcessorsConfigurationSupplier);
        
        assertThat(configurationPerformer.getInitializationTimeout()).isEqualTo(Duration.ofMillis(ONE_MINUTE_IN_MILLIS));
    }
    
    @Test
    public void getInitializationTimeoutShouldReturnValueWhenGiven() throws Exception {
        HierarchicalConfiguration configuration = mock(HierarchicalConfiguration.class);
        ConfigurationProvider configurationProvider = mock(ConfigurationProvider.class);
        when(configurationProvider.getConfiguration("mailetcontainer"))
            .thenReturn(configuration);
        
        Duration expectedDuration = Duration.ofDays(1);
        when(configuration.getLong("initTimeoutInMillis", ONE_MINUTE_IN_MILLIS))
            .thenReturn(expectedDuration.toMillis());
        
        CamelMailetContainerModule.MailetModuleConfigurationPerformer configurationPerformer = new CamelMailetContainerModule.MailetModuleConfigurationPerformer(configurationProvider, 
                camelCompositeProcessor, 
                jamesMailSpooler, 
                mailetContext, 
                mailQueueFactory, 
                transportProcessorCheckSet, 
                defaultProcessorsConfigurationSupplier);
        
        assertThat(configurationPerformer.getInitializationTimeout()).isEqualTo(expectedDuration);
    }
}
