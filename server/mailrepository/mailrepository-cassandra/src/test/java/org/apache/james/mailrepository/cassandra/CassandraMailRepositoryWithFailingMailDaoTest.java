/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.james.mailrepository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableList;
import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.backends.cassandra.components.CassandraModule;
import org.apache.james.backends.cassandra.init.CassandraTypesProvider;
import org.apache.james.backends.cassandra.utils.CassandraUtils;
import org.apache.james.blob.api.BlobId;
import org.apache.james.blob.api.HashBlobId;
import org.apache.james.blob.api.Store;
import org.apache.james.blob.cassandra.BlobTable;
import org.apache.james.blob.cassandra.CassandraBlobModule;
import org.apache.james.blob.cassandra.CassandraBlobsDAO;
import org.apache.james.blob.mail.MimeMessagePartsId;
import org.apache.james.blob.mail.MimeMessageStore;
import org.apache.james.core.MailAddress;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.mailrepository.api.MailKey;
import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.server.core.MailImpl;
import org.apache.mailet.Mail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CassandraMailRepositoryWithFailingMailDaoTest {
    static final MailRepositoryUrl URL = MailRepositoryUrl.from("proto://url");
    static final HashBlobId.Factory BLOB_ID_FACTORY = new HashBlobId.Factory();

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(
        CassandraModule.aggregateModules(
            CassandraMailRepositoryModule.MODULE,
            CassandraBlobModule.MODULE));

    CassandraMailRepository cassandraMailRepository;
    CassandraMailRepositoryKeysDAO keysDAO;

    @BeforeEach
    void setup(CassandraCluster cassandra) {
        FailingMailDAO mailDAO = new FailingMailDAO(cassandra.getConf(), BLOB_ID_FACTORY, cassandra.getTypesProvider());
        keysDAO = new CassandraMailRepositoryKeysDAO(cassandra.getConf(), CassandraUtils.WITH_DEFAULT_CONFIGURATION);
        CassandraMailRepositoryCountDAO countDAO = new CassandraMailRepositoryCountDAO(cassandra.getConf());
        CassandraBlobsDAO blobsDAO = new CassandraBlobsDAO(cassandra.getConf());

        cassandraMailRepository = new CassandraMailRepository(URL,
                keysDAO, countDAO, mailDAO, MimeMessageStore.factory(blobsDAO).mimeMessageStore());
    }

    static class FailingMailDAO extends CassandraMailRepositoryMailDAO {

        public FailingMailDAO(Session session, BlobId.Factory blobIdFactory, CassandraTypesProvider cassandraTypesProvider) {
            super(session, blobIdFactory, cassandraTypesProvider);
        }

        @Override
        public CompletableFuture<Void> store(MailRepositoryUrl url, Mail mail, BlobId headerId, BlobId bodyId) throws MessagingException {
            return CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("Expected failure while storing mail parts");
            });
        }
    }

    @Test
    void keysShouldNotBeStoredWhenStoringMailPartsHasFailed() throws Exception {
        MailKey mailKey = new MailKey("mymail");
        List<MailAddress> recipients = ImmutableList
                .of(new MailAddress("rec1@domain.com"),
                        new MailAddress("rec2@domain.com"));
        MimeMessage mailContent = MimeMessageBuilder.mimeMessageBuilder()
                .setSubject("test")
                .setText("this is the content")
                .build();
        MailImpl mail = new MailImpl(mailKey.asString(), new MailAddress("sender@domain.com"), recipients, mailContent);

        assertThatThrownBy(() -> cassandraMailRepository.store(mail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("java.lang.RuntimeException: Expected failure while storing mail parts");

        assertThat(keysDAO.list(URL).join()).isEmpty();
    }

    @Test
    void mimeMessageShouldBeStoredWhenStoringMailPartsHasFailed(CassandraCluster cassandra) throws Exception {
        MailKey mailKey = new MailKey("mymail");
        List<MailAddress> recipients = ImmutableList
                .of(new MailAddress("rec1@domain.com"),
                        new MailAddress("rec2@domain.com"));
        MimeMessage mailContent = MimeMessageBuilder.mimeMessageBuilder()
                .setSubject("test")
                .setText("this is the content")
                .build();
        MailImpl mail = new MailImpl(mailKey.asString(), new MailAddress("sender@domain.com"), recipients, mailContent);

        assertThatThrownBy(() -> cassandraMailRepository.store(mail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("java.lang.RuntimeException: Expected failure while storing mail parts");

        ResultSet resultSet = cassandra.getConf().execute(select()
                .from(BlobTable.TABLE_NAME));
        assertThat(resultSet.all()).hasSize(2);
    }
}