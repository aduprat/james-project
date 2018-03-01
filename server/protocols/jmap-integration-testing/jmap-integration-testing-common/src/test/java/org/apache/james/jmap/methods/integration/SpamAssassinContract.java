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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.http.client.utils.URIBuilder;
import org.apache.james.GuiceJamesServer;
import org.apache.james.jmap.HttpJmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.mailbox.Role;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.JmapGuiceProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.parsing.Parser;

public interface SpamAssassinContract {

    String NAME = "[0][0]";
    String ARGUMENTS = "[0][1]";
    String BOBS_DOMAIN = "spamer.com";
    String BOB = "bob@" + BOBS_DOMAIN;
    String BOB_PASSWORD = "bobPassword";
    String ALICES_DOMAIN = "angels.org";
    String ALICE = "alice@" + ALICES_DOMAIN;
    String ALICE_PASSWORD = "alicePassword";

    @BeforeEach
    default void setup(GuiceJamesServer jmapServer) throws Throwable {
        jmapServer.start();

        RestAssured.requestSpecification = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
                .setPort(jmapServer.getProbe(JmapGuiceProbe.class).getJmapPort())
                .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.defaultParser = Parser.JSON;

        jmapServer.getProbe(DataProbeImpl.class).addDomain(BOBS_DOMAIN);
        jmapServer.getProbe(DataProbeImpl.class).addDomain(ALICES_DOMAIN);
        jmapServer.getProbe(DataProbeImpl.class).addUser(BOB, BOB_PASSWORD);
        jmapServer.getProbe(DataProbeImpl.class).addUser(ALICE, ALICE_PASSWORD);
    }

    default AccessToken accessTokenFor(GuiceJamesServer jmapServer, String user, String password) {
        return HttpJmapAuthentication.authenticateJamesUser(baseUri(jmapServer), user, password);
    }

    default URIBuilder baseUri(GuiceJamesServer jmapServer) {
        return new URIBuilder()
            .setScheme("http")
            .setHost("localhost")
            .setPort(jmapServer.getProbe(JmapGuiceProbe.class)
                .getJmapPort())
            .setCharset(StandardCharsets.UTF_8);
    }

    @AfterEach
    default void teardown(GuiceJamesServer jmapServer) {
        jmapServer.stop();
    }


    @Test
    default void spamShouldBeDeliveredInSpamMailboxWhenSameMessageHasAlreadyBeenMovedToSpam(GuiceJamesServer jmapServer) throws Exception {
        Duration slowPacedPollInterval = Duration.FIVE_HUNDRED_MILLISECONDS;
        ConditionFactory calmlyAwait = Awaitility.with().pollInterval(slowPacedPollInterval).and().with().pollDelay(slowPacedPollInterval).await();

        // Bob is sending ten times the same message to Alice
        AccessToken bobAccessToken = accessTokenFor(jmapServer, BOB, BOB_PASSWORD);
        int numberOfMessages = 1;
        IntStream.range(0, numberOfMessages)
            .forEach(index -> {
                given()
                    .header("Authorization", bobAccessToken.serialize())
                    .body(setMessageCreate(bobAccessToken))
                .when()
                    .post("/jmap");
            });
        AccessToken aliceAccessToken = accessTokenFor(jmapServer, ALICE, ALICE_PASSWORD);
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getInboxId(aliceAccessToken), numberOfMessages));

        // Alice is moving these messages to Spam -> learning in SpamAssassin
        List<String> messageIds = with()
            .header("Authorization", aliceAccessToken.serialize())
            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getInboxId(aliceAccessToken) + "\"]}}, \"#0\"]]")
        .when()
            .post("/jmap")
        .then()
            .statusCode(200)
            .body(NAME, equalTo("messageList"))
            .body(ARGUMENTS + ".messageIds", hasSize(numberOfMessages))
            .extract()
            .path(ARGUMENTS + ".messageIds");

        messageIds
            .forEach(messageId -> {
                given()
                    .header("Authorization", aliceAccessToken.serialize())
                    .body(String.format("[[\"setMessages\", {\"update\": {\"%s\" : { \"mailboxIds\": [\"" + getSpamId(aliceAccessToken) + "\"] } } }, \"#0\"]]", messageId))
                .when()
                    .post("/jmap")
                .then()
                    .statusCode(200)
                    .body(NAME, equalTo("messagesSet"))
                    .body(ARGUMENTS + ".updated", hasSize(1));
            });
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), numberOfMessages));

        // Bob is sending again the same message to Alice
        given()
            .header("Authorization", bobAccessToken.serialize())
            .body(setMessageCreate(bobAccessToken))
        .when()
            .post("/jmap");

        // This message is delivered in Alice Spam mailbox (she now must have 11 messages in her Spam mailbox)
        calmlyAwait.atMost(30, TimeUnit.SECONDS).until(() -> areMessagesFoundInMailbox(aliceAccessToken, getSpamId(aliceAccessToken), numberOfMessages + 1));
//        with()
//            .header("Authorization", aliceAccessToken.serialize())
//            .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + getSpamId(aliceAccessToken) + "\"]}}, \"#0\"]]")
//        .when()
//            .post("/jmap")
//        .then()
//            .statusCode(200)
//            .body(NAME, equalTo("messageList"))
//            .body(ARGUMENTS + ".messageIds", hasSize(11));
    }

    default boolean areMessagesFoundInMailbox(AccessToken accessToken, String mailboxId, int expectedNumberOfMessages) {
        try {
            with()
                .header("Authorization", accessToken.serialize())
                .body("[[\"getMessageList\", {\"filter\":{\"inMailboxes\":[\"" + mailboxId + "\"]}}, \"#0\"]]")
            .when()
                .post("/jmap")
            .then()
                .statusCode(200)
                .body(NAME, equalTo("messageList"))
                .body(ARGUMENTS + ".messageIds", hasSize(expectedNumberOfMessages));
            return true;

        } catch (AssertionError e) {
            return false;
        }
    }

    default String setMessageCreate(AccessToken accessToken) {
        return "[" +
            "  [" +
            "    \"setMessages\"," +
            "    {" +
            "      \"create\": { \"creationId1337\" : {" +
            "        \"from\": { \"email\": \"" + BOB + "\"}," +
            "        \"to\": [{ \"name\": \"recipient\", \"email\": \"" + ALICE + "\"}]," +
            "        \"subject\": \"Happy News\"," +
            "        \"textBody\": \"This is a SPAM!!!\"," +
            "        \"mailboxIds\": [\"" + getOutboxId(accessToken) + "\"]" +
            "      }}" +
            "    }," +
            "    \"#0\"" +
            "  ]" +
            "]";
    }

    default String getMailboxId(AccessToken accessToken, Role role) {
        return getAllMailboxesIds(accessToken).stream()
            .filter(x -> x.get("role").equalsIgnoreCase(role.serialize()))
            .map(x -> x.get("id"))
            .findFirst().get();
    }

    default List<Map<String, String>> getAllMailboxesIds(AccessToken accessToken) {
        return with()
            .header("Authorization", accessToken.serialize())
            .body("[[\"getMailboxes\", {\"properties\": [\"role\", \"id\"]}, \"#0\"]]")
        .post("/jmap")
            .andReturn()
            .body()
            .jsonPath()
            .getList(ARGUMENTS + ".list");
    }

    default String getInboxId(AccessToken accessToken) {
        return getMailboxId(accessToken, Role.INBOX);
    }

    default String getOutboxId(AccessToken accessToken) {
        return getMailboxId(accessToken, Role.OUTBOX);
    }

    default String getSpamId(AccessToken accessToken) {
        return getMailboxId(accessToken, Role.SPAM);
    }
}
