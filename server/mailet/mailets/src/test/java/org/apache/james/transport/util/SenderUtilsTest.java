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
package org.apache.james.transport.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.james.transport.mailets.redirect.AbstractRedirect;
import org.apache.james.transport.mailets.redirect.SpecialAddress;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.test.FakeMail;
import org.junit.Before;
import org.junit.Test;

public class SenderUtilsTest {

    private AbstractRedirect mailet;
    private SenderUtils testee;

    @Before
    public void setup() {
        mailet = mock(AbstractRedirect.class);
        testee = SenderUtils.from(mailet);
    }

    @Test
    public void getSenderShouldReturnNullWhenMailetSenderIsNull() throws Exception {
        when(mailet.getSender())
            .thenReturn(null);

        FakeMail fakeMail = FakeMail.defaultFakeMail();

        MailAddress sender = testee.getSender(fakeMail);

        assertThat(sender).isNull();
    }

    @Test
    public void getSenderShouldReturnNullWhenMailetSenderEqualsToUnaltered() throws Exception {
        when(mailet.getSender())
            .thenReturn(SpecialAddress.UNALTERED);

        FakeMail fakeMail = FakeMail.defaultFakeMail();

        MailAddress sender = testee.getSender(fakeMail);

        assertThat(sender).isNull();
    }

    @Test
    public void getSenderShouldReturnNullWhenMailetSenderEqualsToSender() throws Exception {
        when(mailet.getSender())
            .thenReturn(SpecialAddress.SENDER);

        FakeMail fakeMail = FakeMail.defaultFakeMail();

        MailAddress sender = testee.getSender(fakeMail);

        assertThat(sender).isNull();
    }

    @Test
    public void getSenderShouldReturnSenderWhenMailetSenderIsCommon() throws Exception {
        MailAddress expectedMailAddress = new MailAddress("sender", "james.org");
        when(mailet.getSender())
            .thenReturn(expectedMailAddress);

        FakeMail fakeMail = FakeMail.defaultFakeMail();

        MailAddress sender = testee.getSender(fakeMail);

        assertThat(sender).isEqualTo(expectedMailAddress);
    }
}
