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

package org.apache.james.transport.mailets.redirect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.mail.MessagingException;

import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class AddressExtractorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MailAddress postmaster;
    private AddressExtractor testee;

    @Before
    public void setup() throws Exception {
        postmaster = new MailAddress("postmaster", "james.org");
        MailetContext mailetContext = mock(MailetContext.class);
        when(mailetContext.getPostmaster())
            .thenReturn(postmaster);

        testee = new AddressExtractor(mailetContext);
    }

    @Test
    public void fromShouldReturnListWhenParsingSucceed() throws Exception {
        List<MailAddress> from = testee.from("user@james.org, user2@james.org", ImmutableList.<String> of());

        assertThat(from).containsOnly(new MailAddress("user", "james.org"),
                new MailAddress("user2", "james.org"));
    }

    @Test
    public void fromShouldReturnSpecialAddressesWhenAddressesAreSpecial() throws Exception {
        List<MailAddress> from = testee.from("postmaster, to", ImmutableList.<String> of("postmaster", "to"));

        assertThat(from).containsOnly(new MailAddress("postmaster", "james.org"),
                new MailAddress("to", "address.marker"));
    }

    @Test
    public void fromShouldThrowWhenParsingFail() throws Exception {
        expectedException.expect(MessagingException.class);
        testee.from("user@james@org", ImmutableList.<String> of());
    }

    @Test
    public void getSpecialAddressShouldReturnAbsentWhenAddressIsNull() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress(null, ImmutableList.<String> of());
        assertThat(specialAddress).isAbsent();
    }

    @Test
    public void getSpecialAddressShouldReturnAbsentWhenAddressIsEmpty() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("", ImmutableList.<String> of());
        assertThat(specialAddress).isAbsent();
    }

    @Test
    public void getSpecialAddressShouldReturnAbsentWhenAddressIsNotSpecial() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("user@james.org", ImmutableList.<String> of());
        assertThat(specialAddress).isAbsent();
    }

    @Test
    public void getSpecialAddressShouldReturnPostmasterWhenAddressMatchesPostmasterSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("postmaster", ImmutableList.of("postmaster"));
        assertThat(specialAddress).contains(postmaster);
    }

    @Test
    public void getSpecialAddressShouldReturnSenderWhenAddressMatchesSenderSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("sender", ImmutableList.of("sender"));
        assertThat(specialAddress).contains(new MailAddress("sender", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnReversePathWhenAddressMatchesReversePathSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("reversepath", ImmutableList.of("reversepath"));
        assertThat(specialAddress).contains(new MailAddress("reverse.path", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnFromWhenAddressMatchesFromSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("from", ImmutableList.of("from"));
        assertThat(specialAddress).contains(new MailAddress("from", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnReplyToWhenAddressMatchesReplyToSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("replyto", ImmutableList.of("replyto"));
        assertThat(specialAddress).contains(new MailAddress("reply.to", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnToWhenAddressMatchesToSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("to", ImmutableList.of("to"));
        assertThat(specialAddress).contains(new MailAddress("to", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnRecipientsWhenAddressMatchesRecipientsSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("recipients", ImmutableList.of("recipients"));
        assertThat(specialAddress).contains(new MailAddress("recipients", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnDeleteWhenAddressMatchesDeleteSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("delete", ImmutableList.of("delete"));
        assertThat(specialAddress).contains(new MailAddress("delete", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnUnalteredWhenAddressMatchesUnalteredSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("unaltered", ImmutableList.of("unaltered"));
        assertThat(specialAddress).contains(new MailAddress("unaltered", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldReturnNullWhenAddressMatchesNullSpecialAddress() throws Exception {
        Optional<MailAddress> specialAddress = testee.getSpecialAddress("null", ImmutableList.of("null"));
        assertThat(specialAddress).contains(new MailAddress("null", "address.marker"));
    }

    @Test
    public void getSpecialAddressShouldThrowWhenSpecialAddressNotAllowed() throws Exception {
        expectedException.expect(MessagingException.class);
        testee.getSpecialAddress("postmaster", ImmutableList.<String> of("notallowed"));
    }
}
