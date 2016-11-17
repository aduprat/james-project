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

public class ReplyToUtilsTest {

    private AbstractRedirect mailet;
    private ReplyToUtils testee;

    @Before
    public void setup() {
        mailet = mock(AbstractRedirect.class);
        testee = ReplyToUtils.from(mailet);
    }

    @Test
    public void getReplyToShouldReturnNullWhenMailetReplyToIsNull() throws Exception {
        when(mailet.getReplyTo())
            .thenReturn(null);

        FakeMail fakeMail = FakeMail.defaultFakeMail();

        MailAddress replyTo = testee.getReplyTo(fakeMail);

        assertThat(replyTo).isNull();
    }

    @Test
    public void getReplyToShouldReturnNullWhenMailetReplyToEqualsToUnaltered() throws Exception {
        when(mailet.getReplyTo())
            .thenReturn(SpecialAddress.UNALTERED);

        FakeMail fakeMail = FakeMail.defaultFakeMail();

        MailAddress replyTo = testee.getReplyTo(fakeMail);

        assertThat(replyTo).isNull();
    }

    @Test
    public void getReplyToShouldReturnSenderWhenMailetReplyToIsCommon() throws Exception {
        MailAddress mailAddress = new MailAddress("test", "james.org");
        when(mailet.getReplyTo())
            .thenReturn(mailAddress);

        MailAddress expectedMailAddress = new MailAddress("sender", "james.org");
        FakeMail fakeMail = FakeMail.builder()
                .sender(expectedMailAddress)
                .build();

        MailAddress replyTo = testee.getReplyTo(fakeMail);

        assertThat(replyTo).isEqualTo(expectedMailAddress);
    }
}
