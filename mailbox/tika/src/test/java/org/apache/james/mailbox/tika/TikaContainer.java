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
package org.apache.james.mailbox.tika;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.james.util.streams.SwarmGenericContainer;
import org.junit.rules.ExternalResource;

import com.google.common.primitives.Ints;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.Duration;
import com.jayway.awaitility.core.ConditionFactory;

public class TikaContainer extends ExternalResource {
    
    private static final int DEFAULT_TIKA_PORT = 9998;
    private static final int DEFAULT_TIMEOUT_IN_MS = Ints.checkedCast(TimeUnit.MINUTES.toMillis(3));

    private static final ConditionFactory CALMLY_AWAIT = Awaitility.with()
            .pollInterval(Duration.FIVE_HUNDRED_MILLISECONDS)
            .and().with()
            .pollDelay(Duration.ONE_HUNDRED_MILLISECONDS)
            .await()
            .atMost(30, TimeUnit.SECONDS);

    private final SwarmGenericContainer tika;

    public TikaContainer() {
        tika = new SwarmGenericContainer("logicalspark/docker-tikaserver:latest");
    }

    public String getIp() {
        return tika.getIp();
    }

    public int getPort() {
        return DEFAULT_TIKA_PORT;
    }

    public int getTimeoutInMillis() {
        return DEFAULT_TIMEOUT_IN_MS;
    }

    public void start() throws Exception {
        tika.start();
        awaitForTika();
    }

    public void stop() {
        tika.stop();
    }

    private void awaitForTika() throws Exception {
        CALMLY_AWAIT.until(() -> isTikaUp());
    }

    private boolean isTikaUp() {
        try {
            URI uri = new URIBuilder()
                    .setHost(tika.getIp())
                    .setPort(DEFAULT_TIKA_PORT)
                    .setScheme("http")
                    .build();
            return Executor.newInstance()
                .execute(Request.Get(uri.resolve("/tika")))
                .returnResponse()
                .getStatusLine()
                .getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
