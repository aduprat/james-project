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

package org.apache.james.jmap.methods.integration;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.with;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.JmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.probe.MailboxProbe;
import org.apache.james.probe.DataProbe;
import org.apache.james.utils.JmapGuiceProbe;
import org.apache.james.utils.PojoDataProbe;
import org.apache.james.utils.PojoMailboxProbe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;

public abstract class GetMessageListMethodTest {
    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");

    protected abstract GuiceJamesServer createJmapServer();

    protected abstract void await();

    private AccessToken accessToken;
    private String username;
    private String domain;
    private GuiceJamesServer jmapServer;
    private MailboxProbe mailboxProbe;
    private DataProbe dataProbe;
    
    @Before
    public void setup() throws Throwable {
        jmapServer = createJmapServer();
        jmapServer.start();
        mailboxProbe = jmapServer.getProbe(PojoMailboxProbe.class);
        dataProbe = jmapServer.getProbe(PojoDataProbe.class);

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(Charsets.UTF_8)))
                .setPort(jmapServer.getProbe(JmapGuiceProbe.class).getJmapPort())
                .build();

        this.domain = "domain.tld";
        this.username = "username@" + domain;
        String password = "password";
        dataProbe.addDomain(domain);
        dataProbe.addUser(username, password);
        this.accessToken = JmapAuthentication.authenticateJamesUser(username, password);
    }

    @After
    public void teardown() {
        jmapServer.stop();
    }
    
    @Test
    public void getMessageListShouldReturnErrorInvalidArgumentsWhenRequestIsInvalid() throws Exception {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\": true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"));
    }

    @Test
    public void getMessageListShouldReturnErrorInvalidArgumentsWhenHeaderIsInvalid() throws Exception {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"header\":[\"132\", \"456\", \"789\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo("invalidArguments"));
    }

    @Test
    public void getMessageListShouldNotFailWhenHeaderIsValid() throws Exception {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"header\":[\"132\", \"456\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesWhenSingleMailboxNoParameters() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder(message1.getMessageId().serialize(), message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesWhenMultipleMailboxesAndNoParameters() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox2"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder(message1.getMessageId().serialize(), message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesOfCurrentUserOnlyWhenMultipleMailboxesAndNoParameters() throws Exception {
        String otherUser = "other@" + domain;
        String password = "password";
        dataProbe.addUser(otherUser, password);

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox2"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, otherUser, "mailbox");
        mailboxProbe.appendMessage(otherUser, new MailboxPath(MailboxConstants.USER_NAMESPACE, otherUser, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder(message1.getMessageId().serialize(), message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenInMailboxesFilterMatches() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        ComposedMessageId message = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        String mailboxId = 
                with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMailboxes\", {}, \"#0\"]]")
                .post("/jmap")
                .path("[0][1].list[0].id");
        
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + mailboxId + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenMultipleInMailboxesFilterMatches() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        ComposedMessageId message = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        await();

        List<String> mailboxIds = 
                with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMailboxes\", {}, \"#0\"]]")
                .post("/jmap")
                .path("[0][1].list.id");
        
        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"%s\", \"%s\"]}}, \"#0\"]]", mailboxIds.get(0), mailboxIds.get(1)))
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenNotInMailboxesFilterMatches() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        MailboxId mailboxId = mailboxProbe.getMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox").getMailboxId();

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        await();
        
        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"getMessageList\", {\"filter\":{\"notInMailboxes\":[\"%s\"]}}, \"#0\"]]", mailboxId.serialize()))
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", empty());
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenNotInMailboxesFilterMatchesTwice() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        MailboxId mailboxId = mailboxProbe.getMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox").getMailboxId();

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox2"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        MailboxId mailbox2Id = mailboxProbe.getMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2").getMailboxId();
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"getMessageList\", {\"filter\":{\"notInMailboxes\":[\"%s\", \"%s\"]}}, \"#0\"]]", mailboxId.serialize(), mailbox2Id.serialize()))
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", empty());
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenIdenticalNotInMailboxesAndInmailboxesFilterMatch() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        MailboxId mailboxId = mailboxProbe.getMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox").getMailboxId();
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"getMessageList\", {\"filter\":{\"notInMailboxes\":[\"%s\"], \"inMailboxes\":[\"%s\"]}}, \"#0\"]]", mailboxId.serialize(), mailboxId.serialize()))
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", empty());
    }

    @Test
    public void getMessageListShouldNotFilterMessagesWhenNotInMailboxesFilterDoesNotMatch() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        ComposedMessageId message = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        MailboxId mailbox2Id = mailboxProbe.getMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2").getMailboxId();
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"getMessageList\", {\"filter\":{\"notInMailboxes\":[\"%s\"]}}, \"#0\"]]", mailbox2Id.serialize()))
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldNotFilterMessagesWhenEmptyNotInMailboxesFilter() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        ComposedMessageId message = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());

        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox2");
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"notInMailboxes\":[]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldFilterMessagesWhenInMailboxesFilterDoesntMatches() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "emptyMailbox");
        MailboxId emptyMailboxId = mailboxProbe.getMailbox(MailboxConstants.USER_NAMESPACE, username, "emptyMailbox").getMailboxId();
        
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), new Date(), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body(String.format("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"%s\"]}}, \"#0\"]]", emptyMailboxId.serialize()))
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(ARGUMENTS + ".messageIds", empty());
    }

    @Test
    public void getMessageListShouldSortMessagesWhenSortedByDateDefault() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"sort\":[\"date\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message1.getMessageId().serialize(), message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldSortMessagesWhenSortedByDateAsc() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"sort\":[\"date asc\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message2.getMessageId().serialize(), message1.getMessageId().serialize()));
    }


    @Test
    public void getMessageListShouldSupportIdSorting() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
            new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
            new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"sort\":[\"id\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder(message2.getMessageId().serialize(), message1.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldSortMessagesWhenSortedByDateDesc() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"sort\":[\"date desc\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message1.getMessageId().serialize(), message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldWorkWhenCollapseThreadIsFalse() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"collapseThreads\":false}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"));
    }
    
    @Test
    public void getMessageListShouldWorkWhenCollapseThreadIsTrue() {
        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"collapseThreads\":true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"));
    }
    
    @Test
    public void getMessageListShouldReturnAllMessagesWhenPositionIsNotGiven() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder(message1.getMessageId().serialize(), message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldReturnSkipMessagesWhenPositionIsGiven() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"position\":1, \"sort\":[\"date desc\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldReturnSkipMessagesWhenPositionAndLimitGiven() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
            new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(2)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
            new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
            new ByteArrayInputStream("Subject: test3\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"position\":1, \"limit\":1, \"sort\":[\"date desc\"]}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldReturnAllMessagesWhenLimitIsNotGiven() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder(message1.getMessageId().serialize(), message2.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldReturnLimitMessagesWhenLimitGiven() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"limit\":1}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", contains(message.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldReturnLimitMessagesWithDefaultValueWhenLimitIsNotGiven() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message1 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        ComposedMessageId message2 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test2\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        ComposedMessageId message3 = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test3\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test4\r\n\r\ntestmail".getBytes()), convertToDate(date), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", containsInAnyOrder(message1.getMessageId().serialize(), message2.getMessageId().serialize(), message3.getMessageId().serialize()));
    }

    @Test
    public void getMessageListShouldChainFetchingMessagesWhenAskedFor() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, username, "mailbox");

        LocalDate date = LocalDate.now();
        ComposedMessageId message = mailboxProbe.appendMessage(username, new MailboxPath(MailboxConstants.USER_NAMESPACE, username, "mailbox"),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()), convertToDate(date.plusDays(1)), false, new Flags());
        await();

        given()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMessageList\", {\"fetchMessages\":true}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body("[0][0]", equalTo("messageList"))
            .body("[1][0]", equalTo("messages"))
            .body("[0][1].messageIds", hasSize(1))
            .body("[0][1].messageIds[0]", equalTo(message.getMessageId().serialize()))
            .body("[1][1].list", hasSize(1))
            .body("[1][1].list[0].id", equalTo(message.getMessageId().serialize()));
    }

    private Date convertToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZONE_ID).toInstant());
    }
}
