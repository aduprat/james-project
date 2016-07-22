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

package org.apache.james.mailbox.elasticsearch.json;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_VALUES;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.Date;

import javax.mail.Flags;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.FlagsBuilder;
import org.apache.james.mailbox.mock.MockMailboxSession;
import org.apache.james.mailbox.store.TestId;
import org.apache.james.mailbox.store.extractor.DefaultTextExtractor;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.mail.model.impl.PropertyBuilder;
import org.apache.james.mailbox.store.mail.model.impl.SimpleMailboxMessage;
import org.apache.james.mailbox.tika.extractor.TikaTextExtractor;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

public class MailboxMessageToElasticSearchJsonTest {

    public static final int SIZE = 25;
    public static final int BODY_START_OCTET = 100;
    public static final TestId MAILBOX_ID = TestId.of(18L);
    public static final long MOD_SEQ = 42L;
    public static final long UID = 25L;
    public static final Charset CHARSET = Charsets.UTF_8;

    private Date date;
    private PropertyBuilder propertyBuilder;

    @Before
    public void setUp() throws Exception {
        // 2015/06/07 00:00:00 0200 (Paris time zone)
        date = new Date(1433628000000L);
        propertyBuilder = new PropertyBuilder();
        propertyBuilder.setMediaType("plain");
        propertyBuilder.setSubType("text");
        propertyBuilder.setTextualLineCount(18L);
        propertyBuilder.setContentDescription("An e-mail");
    }

    @Test
    public void spamEmailShouldBeWellConvertedToJson() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage spamMail = new SimpleMailboxMessage(date,
            SIZE,
            BODY_START_OCTET,
            new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/spamMail.eml"))),
            new Flags(),
            propertyBuilder,
            MAILBOX_ID);
        spamMail.setModSeq(MOD_SEQ);
        assertThatJson(messageToElasticSearchJson.convertToJson(spamMail, ImmutableList.of()))
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResource("eml/spamMail.json"), CHARSET));
    }

    @Test
    public void htmlEmailShouldBeWellConvertedToJson() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage htmlMail = new SimpleMailboxMessage(date,
            SIZE,
            BODY_START_OCTET,
            new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/htmlMail.eml"))),
            new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.SEEN).add("social", "pocket-money").build(),
            propertyBuilder,
            MAILBOX_ID);
        htmlMail.setModSeq(MOD_SEQ);
        htmlMail.setUid(UID);
        assertThatJson(messageToElasticSearchJson.convertToJson(htmlMail, ImmutableList.of()))
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResource("eml/htmlMail.json")));
    }

    @Test
    public void pgpSignedEmailShouldBeWellConvertedToJson() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage pgpSignedMail = new SimpleMailboxMessage(date,
            SIZE,
            BODY_START_OCTET,
            new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/pgpSignedMail.eml"))),
            new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.SEEN).add("debian", "security").build(),
            propertyBuilder,
            MAILBOX_ID);
        pgpSignedMail.setModSeq(MOD_SEQ);
        pgpSignedMail.setUid(UID);
        assertThatJson(messageToElasticSearchJson.convertToJson(pgpSignedMail, ImmutableList.of()))
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResource("eml/pgpSignedMail.json")));
    }

    @Test
    public void simpleEmailShouldBeWellConvertedToJson() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage mail = new SimpleMailboxMessage(date,
            SIZE,
            BODY_START_OCTET,
            new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/mail.eml"))),
            new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.SEEN).add("debian", "security").build(),
            propertyBuilder,
            MAILBOX_ID);
        mail.setModSeq(MOD_SEQ);
        mail.setUid(UID);
        assertThatJson(messageToElasticSearchJson.convertToJson(mail, 
                ImmutableList.of(new MockMailboxSession("user1").getUser(), new MockMailboxSession("user2").getUser())))
            .when(IGNORING_ARRAY_ORDER).when(IGNORING_VALUES)
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResource("eml/mail.json")));
    }

    @Test
    public void recursiveEmailShouldBeWellConvertedToJson() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage recursiveMail = new SimpleMailboxMessage(date,
            SIZE,
            BODY_START_OCTET,
            new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/recursiveMail.eml"))),
            new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.SEEN).add("debian", "security").build(),
            propertyBuilder,
            MAILBOX_ID);
        recursiveMail.setModSeq(MOD_SEQ);
        recursiveMail.setUid(UID);
        assertThatJson(messageToElasticSearchJson.convertToJson(recursiveMail, ImmutableList.of()))
            .when(IGNORING_ARRAY_ORDER).when(IGNORING_VALUES)
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResource("eml/recursiveMail.json")));
    }

    @Test
    public void emailWithNoInternalDateShouldUseNowDate() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage mailWithNoInternalDate = new SimpleMailboxMessage(null,
            SIZE,
            BODY_START_OCTET,
            new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/recursiveMail.eml"))),
            new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.SEEN).add("debian", "security").build(),
            propertyBuilder,
            MAILBOX_ID);
        mailWithNoInternalDate.setModSeq(MOD_SEQ);
        mailWithNoInternalDate.setUid(UID);
        assertThatJson(messageToElasticSearchJson.convertToJson(mailWithNoInternalDate, ImmutableList.of()))
            .when(IGNORING_ARRAY_ORDER)
            .when(IGNORING_VALUES)
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResource("eml/recursiveMail.json")));
    }

    @Test(expected = NullPointerException.class)
    public void emailWithNoMailboxIdShouldThrow() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage mailWithNoMailboxId;
        try {
            mailWithNoMailboxId = new SimpleMailboxMessage(date,
                SIZE,
                BODY_START_OCTET,
                new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/recursiveMail.eml"))),
                new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.SEEN).add("debian", "security").build(),
                propertyBuilder,
                null);
            mailWithNoMailboxId.setModSeq(MOD_SEQ);
            mailWithNoMailboxId.setUid(UID);
        } catch(Exception exception) {
            throw Throwables.propagate(exception);
        }
        messageToElasticSearchJson.convertToJson(mailWithNoMailboxId, ImmutableList.of());
    }

    @Test
    public void getUpdatedJsonMessagePartShouldBehaveWellOnEmptyFlags() throws Exception {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        assertThatJson(messageToElasticSearchJson.getUpdatedJsonMessagePart(new Flags(), MOD_SEQ))
            .isEqualTo("{\"modSeq\":42,\"isAnswered\":false,\"isDeleted\":false,\"isDraft\":false,\"isFlagged\":false,\"isRecent\":false,\"userFlags\":[],\"isUnread\":true}");
    }

    @Test
    public void getUpdatedJsonMessagePartShouldBehaveWellOnNonEmptyFlags() throws Exception {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        assertThatJson(messageToElasticSearchJson.getUpdatedJsonMessagePart(new FlagsBuilder().add(Flags.Flag.DELETED, Flags.Flag.FLAGGED).add("user").build(), MOD_SEQ))
            .isEqualTo("{\"modSeq\":42,\"isAnswered\":false,\"isDeleted\":true,\"isDraft\":false,\"isFlagged\":true,\"isRecent\":false,\"userFlags\":[\"user\"],\"isUnread\":true}");
    }

    @Test(expected = NullPointerException.class)
    public void getUpdatedJsonMessagePartShouldThrowIfFlagsIsNull() throws Exception {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new DefaultTextExtractor(),
            ZoneId.of("Europe/Paris"));
        messageToElasticSearchJson.getUpdatedJsonMessagePart(null, MOD_SEQ);
    }

    @Test
    public void spamEmailShouldBeWellConvertedToJsonWithApacheTika() throws IOException {
        MessageToElasticSearchJson messageToElasticSearchJson = new MessageToElasticSearchJson(
            new TikaTextExtractor(),
            ZoneId.of("Europe/Paris"));
        MailboxMessage spamMail = new SimpleMailboxMessage(date,
            SIZE,
            BODY_START_OCTET,
            new SharedByteArrayInputStream(IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream("eml/nonTextual.eml"))),
            new Flags(),
            propertyBuilder,
            MAILBOX_ID);
        spamMail.setModSeq(MOD_SEQ);
        assertThatJson(messageToElasticSearchJson.convertToJson(spamMail, ImmutableList.of()))
            .when(IGNORING_ARRAY_ORDER)
            .isEqualTo(IOUtils.toString(ClassLoader.getSystemResource("eml/nonTextual.json"), CHARSET));
    }

}
