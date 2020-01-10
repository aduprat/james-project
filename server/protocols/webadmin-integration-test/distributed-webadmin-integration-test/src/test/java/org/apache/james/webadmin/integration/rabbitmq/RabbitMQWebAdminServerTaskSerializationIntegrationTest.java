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

package org.apache.james.webadmin.integration.rabbitmq;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.with;
import static org.apache.james.webadmin.Constants.SEPARATOR;
import static org.apache.james.webadmin.vault.routes.DeletedMessagesVaultRoutes.MESSAGE_PATH_PARAM;
import static org.apache.james.webadmin.vault.routes.DeletedMessagesVaultRoutes.USERS;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.stream.Stream;

import javax.mail.Flags;

import org.apache.james.CassandraExtension;
import org.apache.james.CassandraRabbitMQJamesServerMain;
import org.apache.james.DockerElasticSearchExtension;
import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerBuilder;
import org.apache.james.JamesServerExtension;
import org.apache.james.backends.cassandra.versions.CassandraSchemaVersionManager;
import org.apache.james.backends.cassandra.versions.SchemaVersion;
import org.apache.james.core.Username;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.junit.categories.BasicFeature;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.events.Event;
import org.apache.james.mailbox.events.EventDeadLetters;
import org.apache.james.mailbox.events.GenericGroup;
import org.apache.james.mailbox.events.Group;
import org.apache.james.mailbox.events.MailboxListener;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.model.ComposedMessageId;
import org.apache.james.mailbox.model.MailboxConstants;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.probe.MailboxProbe;
import org.apache.james.mailbox.store.event.EventFactory;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.modules.AwsS3BlobStoreExtension;
import org.apache.james.modules.EventDeadLettersProbe;
import org.apache.james.modules.MailboxProbeImpl;
import org.apache.james.modules.RabbitMQExtension;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.probe.DataProbe;
import org.apache.james.task.TaskManager;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.MailRepositoryProbeImpl;
import org.apache.james.utils.WebAdminGuiceProbe;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.integration.WebadminIntegrationTestModule;
import org.apache.james.webadmin.routes.CassandraMailboxMergingRoutes;
import org.apache.james.webadmin.routes.CassandraMappingsRoutes;
import org.apache.james.webadmin.routes.MailQueueRoutes;
import org.apache.james.webadmin.routes.MailRepositoriesRoutes;
import org.apache.james.webadmin.routes.TasksRoutes;
import org.apache.james.webadmin.vault.routes.DeletedMessagesVaultRoutes;
import org.apache.mailet.base.test.FakeMail;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@Tag(BasicFeature.TAG)
class RabbitMQWebAdminServerTaskSerializationIntegrationTest {

    @RegisterExtension
    static JamesServerExtension testExtension = new JamesServerBuilder()
        .extension(new DockerElasticSearchExtension())
        .extension(new CassandraExtension())
        .extension(new AwsS3BlobStoreExtension())
        .extension(new RabbitMQExtension())
        .server(configuration -> GuiceJamesServer.forConfiguration(configuration)
            .combineWith(CassandraRabbitMQJamesServerMain.MODULES)
            .overrideWith(TestJMAPServerModule.limitToTenMessages())
            .overrideWith(new WebadminIntegrationTestModule()))
        .build();

    private static final String DOMAIN = "domain";
    private static final String USERNAME = "username@" + DOMAIN;

    private DataProbe dataProbe;
    private MailboxProbe mailboxProbe;

    @BeforeEach
    void setUp(GuiceJamesServer guiceJamesServer) throws Exception {
        dataProbe = guiceJamesServer.getProbe(DataProbeImpl.class);
        dataProbe.addDomain(DOMAIN);
        WebAdminGuiceProbe webAdminGuiceProbe = guiceJamesServer.getProbe(WebAdminGuiceProbe.class);

        mailboxProbe = guiceJamesServer.getProbe(MailboxProbeImpl.class);

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminGuiceProbe.getWebAdminPort())
            .build();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void fullReindexingShouldCompleteWhenNoMail() {
        String taskId = with()
            .post("/mailboxes?task=reIndex")
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(notNullValue()))
            .body("type", is("full-reindexing"))
            .body("additionalInformation.successfullyReprocessedMailCount", is(0))
            .body("additionalInformation.failedReprocessedMailCount", is(0))
            .body("additionalInformation.failures", is(anEmptyMap()));
    }

    @Test
    void deleteMailsFromMailQueueShouldCompleteWhenSenderIsValid() {
        String firstMailQueue = with()
                .basePath(MailQueueRoutes.BASE_URL)
            .get()
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getString("[0]");

        String taskId = with()
                .basePath(MailQueueRoutes.BASE_URL)
                .param("sender", USERNAME)
            .delete(firstMailQueue + "/mails")
                .jsonPath()
                .getString("taskId");

        given()
                .basePath(TasksRoutes.BASE)
            .when()
                .get(taskId + "/await")
            .then()
                .body("status", is("completed"))
                .body("taskId", is(notNullValue()))
                .body("type", is("delete-mails-from-mail-queue"))
                .body("additionalInformation.mailQueueName", is(notNullValue()))
                .body("additionalInformation.remainingCount", is(0))
                .body("additionalInformation.initialCount", is(0))
                .body("additionalInformation.sender", is(USERNAME))
                .body("additionalInformation.name", is(nullValue()))
                .body("additionalInformation.recipient", is(nullValue()))
        ;
    }

    @Test
    void reprocessingAllMailsShouldComplete() {
        String escapedRepositoryPath = with()
                .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
            .get()
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getString("[0].path");

        String taskId = with()
                .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
                .param("action", "reprocess")
            .patch(escapedRepositoryPath + "/mails")
            .then()
                .statusCode(HttpStatus.CREATED_201)
                .extract()
                .jsonPath()
                .getString("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(notNullValue()))
            .body("type", is("reprocessing-all"))
            .body("additionalInformation.repositoryPath", is(notNullValue()))
            .body("additionalInformation.targetQueue", is(notNullValue()))
            .body("additionalInformation.targetProcessor", is(nullValue()))
            .body("additionalInformation.initialCount", is(0))
            .body("additionalInformation.remainingCount", is(0));
    }

    @Test
    void reprocessingOneMailShouldCreateATask(GuiceJamesServer guiceJamesServer) throws Exception {
        MailRepositoryStore mailRepositoryStore = guiceJamesServer.getProbe(MailRepositoryProbeImpl.class).getMailRepositoryStore();
        Stream<MailRepositoryUrl> urls = mailRepositoryStore.getUrls();
        MailRepositoryUrl mailRepositoryUrl = urls.findAny().get();
        MailRepository repository = mailRepositoryStore.get(mailRepositoryUrl).get();

        String mailKey = "name1";
        repository.store(FakeMail.builder()
            .name(mailKey)
            .mimeMessage(MimeMessageBuilder.mimeMessageBuilder().build())
            .build());

        String taskId = with()
            .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
            .param("action", "reprocess")
        .patch(mailRepositoryUrl.urlEncoded() + "/mails/name1")
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .extract()
            .jsonPath()
            .getString("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("failed"))
            .body("taskId", is(notNullValue()))
            .body("type", is("reprocessing-one"))
            .body("additionalInformation.repositoryPath", is(mailRepositoryUrl.asString()))
            .body("additionalInformation.targetQueue", is(notNullValue()))
            .body("additionalInformation.mailKey", is(mailKey))
            .body("additionalInformation.targetProcessor", is(nullValue()));
    }

    @Test
    void singleMessageReindexingShouldCompleteWhenMail() throws Exception {
        MailboxId mailboxId = mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX);
        ComposedMessageId composedMessageId = mailboxProbe.appendMessage(
                USERNAME,
                MailboxPath.inbox(Username.of(USERNAME)),
                new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()),
                new Date(),
                false,
                new Flags());

        String taskId = with()
            .post("/mailboxes/" + mailboxId.serialize() + "/mails/"
                    + composedMessageId.getUid().asLong() + "?task=reIndex")
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("message-reindexing"))
            .body("additionalInformation.mailboxId", is(mailboxId.serialize()))
            .body("additionalInformation.uid", is(Math.toIntExact(composedMessageId.getUid().asLong())));
    }

    @Test
    void messageIdReIndexingShouldCompleteWhenMail() throws Exception {
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX);
        ComposedMessageId composedMessageId = mailboxProbe.appendMessage(
            USERNAME,
            MailboxPath.inbox(Username.of(USERNAME)),
            new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()),
            new Date(),
            false,
            new Flags());

        String taskId = with()
            .post("/messages/" + composedMessageId.getMessageId().serialize() + "?task=reIndex")
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("messageId-reindexing"))
            .body("additionalInformation.messageId", is(composedMessageId.getMessageId().serialize()));
    }

    @Test
    void userReindexingShouldComplete() {
        String taskId = with()
                .queryParam("task", "reIndex")
            .post("users/" + USERNAME + "/mailboxes")
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("user-reindexing"))
            .body("additionalInformation.successfullyReprocessedMailCount", is(0))
            .body("additionalInformation.failedReprocessedMailCount", is(0))
            .body("additionalInformation.username", is(USERNAME))
            .body("additionalInformation.failures", is(anEmptyMap()));
    }

    @Test
    void deletedMessageVaultRestoreShouldComplete() throws Exception {
        dataProbe.addUser(USERNAME, "password");
        String query =
            "{" +
                "  \"fieldName\": \"subject\"," +
                "  \"operator\": \"contains\"," +
                "  \"value\": \"subject contains\"" +
                "}";

        String taskId =
            with()
                .basePath(DeletedMessagesVaultRoutes.ROOT_PATH)
                .queryParam("action", "restore")
                .body(query)
            .post(USERS + SEPARATOR + USERNAME)
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("deleted-messages-restore"))
            .body("additionalInformation.username", is(USERNAME))
            .body("additionalInformation.successfulRestoreCount", is(0))
            .body("additionalInformation.errorRestoreCount", is(0));
    }

    @Test
    void deletedMessageVaultExportShouldComplete() throws Exception {
        dataProbe.addUser(USERNAME, "password");
        String query = "{" +
            "\"combinator\": \"and\"," +
            "\"criteria\": []" +
            "}";

        String exportTo = "exportTo@james.org";
        String taskId = with()
            .basePath(DeletedMessagesVaultRoutes.ROOT_PATH)
            .queryParam("action", "export")
            .queryParam("exportTo", exportTo)
            .body(query)
        .post(USERS + SEPARATOR + USERNAME)
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .extract()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("deleted-messages-export"))
            .body("additionalInformation.userExportFrom", is(USERNAME))
            .body("additionalInformation.exportTo", is(exportTo))
            .body("additionalInformation.totalExportedMessages", is(0));
    }

    @Test
    void errorRecoveryIndexationShouldCompleteWhenNoMail() {
        String taskId = with()
            .post("/mailboxes?task=reIndex")
            .jsonPath()
            .get("taskId");

        with()
            .basePath(TasksRoutes.BASE)
            .get(taskId + "/await");

        String fixingTaskId = with()
            .queryParam("reIndexFailedMessagesOf", taskId)
            .queryParam("task", "reIndex")
        .post("/mailboxes")
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .extract()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(fixingTaskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("error-recovery-indexation"))
            .body("additionalInformation.successfullyReprocessedMailCount", is(0))
            .body("additionalInformation.failedReprocessedMailCount", is(0))
            .body("additionalInformation.failures", is(anEmptyMap()));
    }

    @Test
    void eventDeadLettersRedeliverShouldComplete() {
        String taskId = with()
            .queryParam("action", "reDeliver")
        .post("/events/deadLetter")
            .then()
            .statusCode(HttpStatus.CREATED_201)
            .extract()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("event-dead-letters-redeliver-all"))
            .body("additionalInformation.successfulRedeliveriesCount", is(0))
            .body("additionalInformation.failedRedeliveriesCount", is(0));

    }

    @Test
    void eventDeadLettersRedeliverShouldCreateATask(GuiceJamesServer guiceJamesServer) {
        String uuid = "6e0dd59d-660e-4d9b-b22f-0354479f47b4";
        String insertionUuid = "6e0dd59d-660e-4d9b-b22f-0354479f47b7";
        Group group = new GenericGroup("a");
        EventDeadLetters.InsertionId insertionId = EventDeadLetters.InsertionId.of(insertionUuid);
        MailboxListener.MailboxAdded event = EventFactory.mailboxAdded()
            .eventId(Event.EventId.of(uuid))
            .user(Username.of(USERNAME))
            .sessionId(MailboxSession.SessionId.of(452))
            .mailboxId(InMemoryId.of(453))
            .mailboxPath(MailboxPath.forUser(Username.of(USERNAME), "Important-mailbox"))
            .build();

        guiceJamesServer
            .getProbe(EventDeadLettersProbe.class)
            .getEventDeadLetters()
            .store(group, event, insertionId)
            .block();

        String taskId = with()
            .queryParam("action", "reDeliver")
        .post("/events/deadLetter/groups/" + group.asString())
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .extract()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("failed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("event-dead-letters-redeliver-group"))
            .body("additionalInformation.successfulRedeliveriesCount", is(0))
            .body("additionalInformation.failedRedeliveriesCount", is(0))
            .body("additionalInformation.group", is(group.asString()));
    }

    @Test
    void postRedeliverSingleEventShouldCreateATask(GuiceJamesServer guiceJamesServer) {
        String uuid = "6e0dd59d-660e-4d9b-b22f-0354479f47b4";
        String insertionUuid = "6e0dd59d-660e-4d9b-b22f-0354479f47b7";
        Group group = new GenericGroup("a");
        EventDeadLetters.InsertionId insertionId = EventDeadLetters.InsertionId.of(insertionUuid);
        MailboxListener.MailboxAdded event = EventFactory.mailboxAdded()
            .eventId(Event.EventId.of(uuid))
            .user(Username.of(USERNAME))
            .sessionId(MailboxSession.SessionId.of(452))
            .mailboxId(InMemoryId.of(453))
            .mailboxPath(MailboxPath.forUser(Username.of(USERNAME), "Important-mailbox"))
            .build();

        guiceJamesServer
            .getProbe(EventDeadLettersProbe.class)
            .getEventDeadLetters()
            .store(group, event, insertionId)
            .block();

        String taskId = with()
            .queryParam("action", "reDeliver")
        .post("/events/deadLetter/groups/" + group.asString() + "/" + insertionUuid)
        .then()
            .statusCode(HttpStatus.CREATED_201)
            .extract()
            .jsonPath()
            .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("failed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("event-dead-letters-redeliver-one"))
            .body("additionalInformation.successfulRedeliveriesCount", is(0))
            .body("additionalInformation.failedRedeliveriesCount", is(0))
            .body("additionalInformation.group", is(group.asString()))
            .body("additionalInformation.insertionId", is(insertionId.getId().toString()));
    }

    @Test
    void clearMailQueueShouldCompleteWhenNoQueryParameters() {
        String firstMailQueue = with()
                .basePath(MailQueueRoutes.BASE_URL)
            .get()
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getString("[0]");

        String taskId = with()
                .basePath(MailQueueRoutes.BASE_URL)
            .delete(firstMailQueue + "/mails")
                .jsonPath()
                .getString("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("clear-mail-queue"))
            .body("additionalInformation.mailQueueName", is(notNullValue()))
            .body("additionalInformation.initialCount", is(0))
            .body("additionalInformation.remainingCount", is(0));
    }

    @Test
    void blobStoreVaultGarbageCollectionShouldComplete() {
        String taskId =
            with()
                .basePath(DeletedMessagesVaultRoutes.ROOT_PATH)
                .queryParam("scope", "expired")
            .delete()
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(taskId))
            .body("type", is("deleted-messages-blob-store-based-garbage-collection"))
            .body("additionalInformation.beginningOfRetentionPeriod", is(notNullValue()))
            .body("additionalInformation.deletedBuckets", is(empty()));
    }

    @Test
    void clearMailRepositoryShouldComplete() {
        String escapedRepositoryPath = with()
                .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
            .get()
            .then()
                .statusCode(HttpStatus.OK_200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .jsonPath()
                .getString("[0].path");

        String taskId = with()
                .basePath(MailRepositoriesRoutes.MAIL_REPOSITORIES)
            .delete(escapedRepositoryPath + "/mails")
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(taskId))
            .body("type", is("clear-mail-repository"))
            .body("additionalInformation.repositoryPath", is(notNullValue()))
            .body("additionalInformation.initialCount", is(0))
            .body("additionalInformation.remainingCount", is(0));
    }

    @Test
    void mailboxMergingShouldComplete() {
        MailboxId origin = mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX);
        MailboxId destination = mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX + "2");

        String taskId = given()
                .body("{" +
                    "    \"mergeOrigin\":\"" + origin.serialize() + "\"," +
                    "    \"mergeDestination\":\"" + destination.serialize() + "\"" +
                    "}")
            .post(CassandraMailboxMergingRoutes.BASE)
                .jsonPath()
                .getString("taskId");

        with()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is(TaskManager.Status.COMPLETED.getValue()))
            .body("taskId", is(taskId))
            .body("type", is("mailbox-merging"))
            .body("additionalInformation.oldMailboxId", is(origin.serialize()))
            .body("additionalInformation.newMailboxId", is(destination.serialize()))
            .body("additionalInformation.totalMessageCount", is(0))
            .body("additionalInformation.messageMovedCount", is(0))
            .body("additionalInformation.messageFailedCount", is(0));
    }

    @Test
    void singleMailboxReindexingShouldComplete() {
        MailboxId mailboxId = mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX);

        String taskId = when()
            .post("/mailboxes/" + mailboxId.serialize() + "?task=reIndex")
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(Matchers.notNullValue()))
            .body("type", is("mailbox-reindexing"))
            .body("additionalInformation.successfullyReprocessedMailCount", is(0))
            .body("additionalInformation.failedReprocessedMailCount", is(0))
            .body("additionalInformation.mailboxId", is(mailboxId.serialize()))
            .body("additionalInformation.failures", is(anEmptyMap()));
    }

    @Test
    void deletedMessagesVaultDeleteShouldCompleteEvenNoDeletedMessageExisted() throws Exception {
        dataProbe.addUser(USERNAME, "password");
        mailboxProbe.createMailbox(MailboxConstants.USER_NAMESPACE, USERNAME, MailboxConstants.INBOX);
        ComposedMessageId composedMessageId = mailboxProbe.appendMessage(
            USERNAME,
            MailboxPath.inbox(Username.of(USERNAME)),
            new ByteArrayInputStream("Subject: test\r\n\r\ntestmail".getBytes()),
            new Date(),
            false,
            new Flags());

        String taskId =
            with()
                .basePath(DeletedMessagesVaultRoutes.ROOT_PATH)
            .delete(USERS + SEPARATOR + USERNAME + SEPARATOR + MESSAGE_PATH_PARAM + SEPARATOR + composedMessageId.getMessageId().serialize())
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(taskId))
            .body("type", is("deleted-messages-delete"))
            .body("additionalInformation.username", is(USERNAME))
            .body("additionalInformation.deleteMessageId", is(composedMessageId.getMessageId().serialize()));
    }

    @Test
    void cassandraMigrationShouldComplete() {
        SchemaVersion toVersion = CassandraSchemaVersionManager.MAX_VERSION;
        String taskId = with()
                .body(String.valueOf(toVersion.getValue()))
            .post("cassandra/version/upgrade")
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(taskId))
            .body("type", is("cassandra-migration"))
            .body("additionalInformation.toVersion", is(toVersion.getValue()));
    }

    @Test
    void cassandraMappingsSolveInconsistenciesShouldComplete() {
        String taskId = with()
                .basePath(CassandraMappingsRoutes.ROOT_PATH)
                .queryParam("action", "SolveInconsistencies")
            .post()
                .jsonPath()
                .get("taskId");

        given()
            .basePath(TasksRoutes.BASE)
        .when()
            .get(taskId + "/await")
        .then()
            .body("status", is("completed"))
            .body("taskId", is(taskId))
            .body("type", is("cassandra-mappings-solve-inconsistencies"))
            .body("additionalInformation.successfulMappingsCount", is(0))
            .body("additionalInformation.errorMappingsCount", is(0));
    }
}