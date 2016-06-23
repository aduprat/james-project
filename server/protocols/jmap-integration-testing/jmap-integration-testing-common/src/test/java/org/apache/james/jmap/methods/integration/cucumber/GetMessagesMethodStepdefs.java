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

package org.apache.james.jmap.methods.integration.cucumber;

import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.mail.Flags;

import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxPath;
import org.javatuples.Pair;

import com.google.common.base.Joiner;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import cucumber.runtime.java.guice.ScenarioScoped;
import groovy.json.StringEscapeUtils;

@ScenarioScoped
public class GetMessagesMethodStepdefs {

    private static final String NAME = "[0][0]";
    private static final String ARGUMENTS = "[0][1]";
    private static final String FIRST_MESSAGE = ARGUMENTS + ".list[0]";
    private static final String ATTACHMENTS = FIRST_MESSAGE + ".attachments";
    private static final String FIRST_ATTACHMENT = ATTACHMENTS + "[0]";
    private static final String SECOND_ATTACHMENT = ATTACHMENTS + "[1]";


    private final MainStepdefs mainStepdefs;
    private final UserStepdefs userStepdefs;

    private Response post;
    private ValidatableResponse response;

    @Inject
    private GetMessagesMethodStepdefs(MainStepdefs mainStepdefs, UserStepdefs userStepdefs) {
        this.mainStepdefs = mainStepdefs;
        this.userStepdefs = userStepdefs;
    }

    @Given("^the user has a message in \"([^\"]*)\" mailbox with subject \"([^\"]*)\" and content \"([^\"]*)\"$")
    public void appendMessage(String mailbox, String subject, String content) throws Throwable {
        appendMessage(mailbox, Optional.empty(), subject, content, Optional.empty());
    }

    @Given("^the user has a message in \"([^\"]*)\" mailbox with content-type \"([^\"]*)\" subject \"([^\"]*)\" and content \"([^\"]*)\"$")
    public void appendMessage(String mailbox, String contentType, String subject, String content) throws Throwable {
        appendMessage(mailbox, Optional.of(contentType), subject, content, Optional.empty());
    }

    @Given("^the user has a message in \"([^\"]*)\" mailbox with subject \"([^\"]*)\" and content \"([^\"]*)\" with headers$")
    public void appendMessage(String mailbox, String subject, String content, DataTable headers) throws Throwable {
        appendMessage(mailbox, Optional.empty(), subject, content, Optional.of(headers.asMap(String.class, String.class)));
    }

    private void appendMessage(String mailbox, Optional<String> contentType, String subject, String content, Optional<Map<String, String>> headers) throws Exception {
        ZonedDateTime dateTime = ZonedDateTime.parse("2014-10-30T14:12:00Z");
        mainStepdefs.jmapServer.serverProbe().appendMessage(userStepdefs.username, 
                new MailboxPath(MailboxConstants.USER_NAMESPACE, userStepdefs.username, mailbox),
                new ByteArrayInputStream(message(contentType, subject, content, headers).getBytes()), 
                Date.from(dateTime.toInstant()), false, new Flags());
    }

    private String message(Optional<String> contentType, String subject, String content, Optional<Map<String,String>> headers) {
        Function<Set<Map.Entry<String, String>>, String> entriesToString = entries -> entries.stream()
                .map(this::entryToPair)
                .map(this::joinKeyValue)
                .collect(Collectors.joining("\r\n", "", "\r\n"));

        String headersAsString = headers
                .map(map -> map.entrySet())
                .map(entriesToString)
                .orElse("");
                
        String contentTypeAsString = contentType
                .map(value -> "Content-Type: " + value + "\r\n")
                .orElse("");

        return headersAsString + contentTypeAsString + "Subject: " + subject + "\r\n\r\n" + content;
    }

    @Given("^the user has a message in \"([^\"]*)\" mailbox with two attachments$")
    public void appendHtmlMessageWithTwoAttachments(String mailbox) throws Throwable {
        appendMessage("eml/twoAttachments.eml");
    }

    @Given("^the user has a message in \"([^\"]*)\" mailbox with two attachments in text$")
    public void appendTextMessageWithTwoAttachments(String arg1) throws Throwable {
        appendMessage("eml/twoAttachmentsTextPlain.eml");
    }

    @Given("^the user has a multipart message in \"([^\"]*)\" mailbox$")
    public void appendMultipartMessageWithOneAttachments(String arg1) throws Throwable {
        appendMessage("eml/htmlAndTextMultipartWithOneAttachment.eml");
    }

    private void appendMessage(String emlFileName) throws Exception {
        ZonedDateTime dateTime = ZonedDateTime.parse("2014-10-30T14:12:00Z");
        mainStepdefs.jmapServer.serverProbe().appendMessage(userStepdefs.username, 
                new MailboxPath(MailboxConstants.USER_NAMESPACE, userStepdefs.username, "inbox"),
                ClassLoader.getSystemResourceAsStream(emlFileName), 
                Date.from(dateTime.toInstant()), false, new Flags());
    }

    @When("^the user is getting his messages and gives a non null accountId$")
    public void postWithAccountId() throws Exception {
        post("[[\"getMessages\", {\"accountId\": \"1\"}, \"#0\"]]");
    }

    @When("^the user is getting his messages and gives unknown arguments$")
    public void postWithUnknownArguments() throws Exception {
        post("[[\"getMessages\", {\"WAT\": true}, \"#0\"]]");
    }

    @When("^the user is getting his messages and gives invalid argument$")
    public void postWithInvalidArguments() throws Exception {
        post("[[\"getMessages\", {\"ids\": null}, \"#0\"]]");
    }

    @When("^the user is getting his messages$")
    public void post() throws Throwable {
        post("[[\"getMessages\", {\"ids\": []}, \"#0\"]]");
    }

    @When("^the user is getting his messages and gives a list of ids \"(.*?)\"$")
    public void postWithAListOfIds(String ids) throws Throwable {
        post("[[\"getMessages\", {\"ids\": " + ids + "}, \"#0\"]]");
    }

    @When("^the user is getting his messages with parameters$")
    public void postWithParameters(DataTable parameters) throws Throwable {
        String payload = 
                parameters.asMap(String.class, String.class)
                    .entrySet()
                    .stream()
                    .map(this::entryToPair)
                    .map(this::quoteIndex)
                    .map(this::joinKeyValue)
                    .collect(Collectors.joining(",", "{", "}"));
        
        post("[[\"getMessages\", " + payload + ", \"#0\"]]");
    }

    private Pair<String, String> entryToPair(Map.Entry<String, String> entry) {
        return Pair.with(entry.getKey(), entry.getValue());
    }

    private Pair<String, String> quoteIndex(Pair<String, String> pair) {
        return Pair.with(String.format("\"%s\"", pair.getValue0()), pair.getValue1());
    }

    private String joinKeyValue(Pair<String, String> pair) {
        return Joiner.on(": ").join(pair);
    }

    private void post(String requestBody) {
        post = with()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .header("Authorization", userStepdefs.accessToken.serialize())
            .body(requestBody)
            .post("/jmap");
    }

    @Then("^an error \"([^\"]*)\" is returned$")
    public void error(String type) throws Throwable {
        response = post.then()
            .statusCode(200)
            .body(NAME, equalTo("error"))
            .body(ARGUMENTS + ".type", equalTo(type));
    }

    @Then("^no error is returned$")
    public void noError() throws Throwable {
        response = post.then()
            .statusCode(200)
            .body(NAME, equalTo("messages"));
    }

    @Then("^the list of unknown messages is empty$")
    public void assertNotFoundIsEmpty() {
        response.body(ARGUMENTS + ".notFound", empty());
    }

    @Then("^the list of messages is empty$")
    public void assertListIsEmpty() {
        response.body(ARGUMENTS + ".list", empty());
    }

    @Then("^the description is \"(.*?)\"$")
    public void assertDescription(String description) throws Throwable {
        response.body(ARGUMENTS + ".description", equalTo(description));
    }

    @Then("^the notFound list should contains \"([^\"]*)\"$")
    public void assertNotFoundListContains(String ids) throws Throwable {
        response.body(ARGUMENTS + ".notFound", contains(ids));
    }

    @Then("^the list should contain (\\d+) message$")
    public void assertListContains(int numberOfMessages) throws Throwable {
        response.body(ARGUMENTS + ".list", hasSize(numberOfMessages));
    }
    
    @Then("^the id of the first message is \"([^\"]*)\"$")
    public void assertIdOfTheFirstMessage(String id) throws Throwable {
        response.body(FIRST_MESSAGE + ".id", equalTo(id));
    }

    @Then("^the threadId of the first message is \"([^\"]*)\"$")
    public void assertThreadIdOfTheFirstMessage(String threadId) throws Throwable {
        response.body(FIRST_MESSAGE + ".threadId", equalTo(threadId));
    }

    @Then("^the subject of the first message is \"([^\"]*)\"$")
    public void assertSubjectOfTheFirstMessage(String subject) throws Throwable {
        response.body(FIRST_MESSAGE + ".subject", equalTo(subject));
    }

    @Then("^the textBody of the first message is \"([^\"]*)\"$")
    public void assertTextBodyOfTheFirstMessage(String textBody) throws Throwable {
        response.body(FIRST_MESSAGE + ".textBody", equalTo(StringEscapeUtils.unescapeJava(textBody)));
    }

    @Then("^the htmlBody of the first message is \"([^\"]*)\"$")
    public void assertHtmlBodyOfTheFirstMessage(String htmlBody) throws Throwable {
        response.body(FIRST_MESSAGE + ".htmlBody", equalTo(StringEscapeUtils.unescapeJava(htmlBody)));
    }

    @Then("^the isUnread of the first message is \"([^\"]*)\"$")
    public void assertIsUnreadOfTheFirstMessage(String isUnread) throws Throwable {
        response.body(FIRST_MESSAGE + ".isUnread", equalTo(Boolean.valueOf(isUnread)));
    }

    @Then("^the preview of the first message is \"([^\"]*)\"$")
    public void assertPreviewOfTheFirstMessage(String preview) throws Throwable {
        response.body(FIRST_MESSAGE + ".preview", equalTo(StringEscapeUtils.unescapeJava(preview)));
    }

    @Then("^the headers of the first message is map containing only$")
    public void assertHeadersOfTheFirstMessage(DataTable headers) throws Throwable {
        response.body(FIRST_MESSAGE + ".headers", equalTo(headers.asMap(String.class, String.class)));
    }

    @Then("^the date of the first message is \"([^\"]*)\"$")
    public void assertDateOfTheFirstMessage(String date) throws Throwable {
        response.body(FIRST_MESSAGE + ".date", equalTo(date));
    }

    @Then("^the hasAttachment of the first message is \"([^\"]*)\"$")
    public void assertHasAttachmentOfTheFirstMessage(String hasAttachment) throws Throwable {
        response.body(FIRST_MESSAGE + ".hasAttachment", equalTo(Boolean.valueOf(hasAttachment)));
    }

    @Then("^the list of attachments of the first message is empty$")
    public void assertAttachmentsOfTheFirstMessageIsEmpty() throws Throwable {
        response.body(ATTACHMENTS, empty());
    }

    @Then("^the property \"([^\"]*)\" of the first message is null$")
    public void assertPropertyIsNull(String property) throws Throwable {
        response.body(FIRST_MESSAGE + "." + property, nullValue());
    }

    @Then("^the list of attachments of the first message contains (\\d+) attachments?$")
    public void assertAttachmentsHasSize(int numberOfAttachments) throws Throwable {
        response.body(ATTACHMENTS, hasSize(numberOfAttachments));
    }

    @Then("^the first attachment blobId is \"([^\"]*)\"$")
    public void assertFirstAttachmentBlobId(String blobId) throws Throwable {
        response.body(FIRST_ATTACHMENT + ".blobId", equalTo(blobId));
    }

    @Then("^the first attachment type is \"([^\"]*)\"$")
    public void assertFirstAttachmentType(String type) throws Throwable {
        response.body(FIRST_ATTACHMENT + ".type", equalTo(type));
    }

    @Then("^the first attachment size is (\\d+)$")
    public void assertFirstAttachmentSize(int size) throws Throwable {
        response.body(FIRST_ATTACHMENT + ".size", equalTo(size));
    }

    @Then("^the second attachment blobId is \"([^\"]*)\"$")
    public void assertSecondAttachmentBlobId(String blobId) throws Throwable {
        response.body(SECOND_ATTACHMENT + ".blobId", equalTo(blobId));
    }

    @Then("^the second attachment type is \"([^\"]*)\"$")
    public void assertSecondAttachmentType(String type) throws Throwable {
        response.body(SECOND_ATTACHMENT + ".type", equalTo(type));
    }

    @Then("^the second attachment size is (\\d+)$")
    public void assertSecondAttachmentSize(int size) throws Throwable {
        response.body(SECOND_ATTACHMENT + ".size", equalTo(size));
    }
}
