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

package org.apache.james.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.james.jmap.HttpJmapAuthentication;
import org.apache.james.jmap.api.access.AccessToken;
import org.apache.james.utils.IMAPMessageReader;
import org.apache.james.utils.SMTPMessageSender;
import org.awaitility.Duration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;

interface DeploymentTest {
    String LOCALHOST = "localhost";
    String SIMPSON = "simpson";
    String HOMER = "homer@" + SIMPSON;
    String HOMER_PASSWORD = "secret";
    String BART = "bart@" + SIMPSON;
    String BART_PASSWORD = "tellnobody";
    int JMAP_PORT = 80;
    int SMTP_PORT = 25;
    int IMAP_PORT = 143;
    int WEBADMIN_PORT = 8000;

    @Test
    default void shouldHaveAllServicesResponding(JamesContainer james) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, URISyntaxException {
        initiateRestAssured(james);
        URIBuilder jmapApi = getJmapURI(james);

        registerDomain();
        registerHomer();
        registerBart();

        sendMessageFromBartToHomer(james);
        assertImapMessageReceived(james);

        AccessToken homerAccessToken = HttpJmapAuthentication.authenticateJamesUser(jmapApi, HOMER, HOMER_PASSWORD);
        assertJmapWorks(homerAccessToken, jmapApi);
        assertJmapSearchWork(homerAccessToken, jmapApi);
    }

    default void initiateRestAssured(GenericContainer<?> james) {
        RestAssured.requestSpecification = new RequestSpecBuilder()
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(newConfig().encoderConfig(encoderConfig().defaultContentCharset(StandardCharsets.UTF_8)))
            .setPort(james.getMappedPort(WEBADMIN_PORT))
            .build();
    }

    default URIBuilder getJmapURI(GenericContainer<?> james) {
        return new URIBuilder().setScheme("http").setHost(LOCALHOST).setPort(james.getMappedPort(JMAP_PORT));
    }

    default void registerDomain() {
        when()
            .put("/domains/" + SIMPSON)
        .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    default void registerHomer() {
        given()
            .body(String.format("{\"password\": \"%s\"}", HOMER_PASSWORD))
        .when()
            .put("/users/" + HOMER)
        .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    default void registerBart() {
        given()
            .body(String.format("{\"password\": \"%s\"}", BART_PASSWORD))
        .when()
            .put("/users/" + BART)
        .then()
            .statusCode(HTTP_NO_CONTENT);
    }

    default void sendMessageFromBartToHomer(GenericContainer<?> james)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        try (SMTPMessageSender smtp = new SMTPMessageSender(SIMPSON)) {
            smtp.connect(LOCALHOST, james.getMappedPort(SMTP_PORT))
                .authenticate(BART, BART_PASSWORD)
                .sendMessage(BART, HOMER);
        }
    }

    default void assertImapMessageReceived(GenericContainer<?> james) throws IOException {
        try (IMAPMessageReader imapMessageReader = new IMAPMessageReader()) {
            imapMessageReader.connect(LOCALHOST, james.getMappedPort(IMAP_PORT))
                .login(HOMER, HOMER_PASSWORD)
                .select("INBOX");

            await().atMost(Duration.TEN_SECONDS).until(imapMessageReader::hasAMessage);
            assertThat(imapMessageReader.readFirstMessage()).contains("FROM: " + BART);
        }
    }

    default void assertJmapWorks(AccessToken homerAccessToken, URIBuilder jmapApi) throws IOException, URISyntaxException {
        Content lastMessageId = Request.Post(jmapApi.setPath("/jmap").build())
            .addHeader("Authorization", homerAccessToken.serialize())
            .bodyString("[[\"getMessageList\", {\"sort\":[\"date desc\"]}, \"#0\"]]", org.apache.http.entity.ContentType.APPLICATION_JSON)
            .execute()
            .returnContent();
        
        JsonAssertions.assertThatJson(lastMessageId.asString(StandardCharsets.UTF_8))
            .inPath("$..messageIds[*]")
            .isArray()
            .hasSize(1);
    }

    default void assertJmapSearchWork(AccessToken homerAccessToken, URIBuilder jmapApi) throws IOException, URISyntaxException {
        Content searchResult = Request.Post(jmapApi.setPath("/jmap").build())
                .addHeader("Authorization", homerAccessToken.serialize())
                .bodyString("[[\"getMessageList\", {\"filter\" : {\"text\": \"content\"}}, \"#0\"]]", org.apache.http.entity.ContentType.APPLICATION_JSON)
                .execute()
                .returnContent();
        
        JsonAssertions.assertThatJson(searchResult.asString(StandardCharsets.UTF_8))
            .inPath("$..messageIds[*]")
            .isArray()
            .hasSize(1);
    }
}
