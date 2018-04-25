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
package org.apache.james.modules.mailbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.james.mailbox.store.event.MailboxListenerRegistry;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

public class GlobalMailboxListenersTest {

    private Injector injector;
    private MailboxListenerRegistry registry;
    private GlobalMailboxListeners testee;

    @Before
    public void setup() {
        injector = mock(Injector.class);
        registry = new MailboxListenerRegistry();
        testee = new GlobalMailboxListeners(injector, registry);
    }

    @Test
    public void configureListenerShouldThrowWhenClassIsNotInTheConfiguration() {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();

        assertThatThrownBy(() -> testee.configureListener(configuration))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void configureListenerShouldThrowWhenClassIsEmpty() {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();
        configuration.addProperty("class", "");

        assertThatThrownBy(() -> testee.configureListener(configuration))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void configureListenerShouldThrowWhenClassCantBeLoaded() {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();
        configuration.addProperty("class", "MyUnknownClass");

        assertThatThrownBy(() -> testee.configureListener(configuration))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void configureListenerShouldThrowWhenClassCantBeCastToMailboxListener() {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();
        configuration.addProperty("class", "java.lang.String");

        assertThatThrownBy(() -> testee.configureListener(configuration))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void configureListenerShouldThrowWhenNotFullClassName() {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();
        configuration.addProperty("class", "NoopMailboxListener");

        assertThatThrownBy(() -> testee.configureListener(configuration))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void configureListenerShouldAddMailboxListenerWhenConfigurationIsGood() {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();
        configuration.addProperty("class", "org.apache.james.modules.mailbox.NoopMailboxListener");

        when(injector.getInstance(NoopMailboxListener.class))
            .thenReturn(new NoopMailboxListener());

        testee.configureListener(configuration);

        assertThat(registry.getGlobalListeners()).hasSize(1);
    }

    @Test
    public void configureShouldAddMailboxListenersWhenConfigurationIsGood() throws ConfigurationException {
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder();
        String listeners = 
                "<listeners>" +
                    "<listener>" +
                        "<class>org.apache.james.modules.mailbox.NoopMailboxListener</class>" +
                    "</listener>" +
                    "<listener>" +
                        "<class>org.apache.james.modules.mailbox.NoopMailboxListener</class>" +
                    "</listener>" +
                "</listeners>";
        configuration.load(new ByteArrayInputStream(listeners.getBytes(StandardCharsets.UTF_8)));

        when(injector.getInstance(NoopMailboxListener.class))
            .thenReturn(new NoopMailboxListener());

        testee.configure(configuration);

        assertThat(registry.getGlobalListeners()).hasSize(2);
    }
}
