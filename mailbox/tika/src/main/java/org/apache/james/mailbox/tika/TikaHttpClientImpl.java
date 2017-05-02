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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;

import com.google.common.base.Charsets;

public class TikaHttpClientImpl implements TikaHttpClient {

    private static final String RMETA_AS_TEXT_ENDPOINT = "/rmeta/text";

    private final TikaConfiguration tikaConfiguration;
    private final Executor executor;
    private final URI uri;

    public TikaHttpClientImpl(TikaConfiguration tikaConfiguration) throws URISyntaxException {
        this.tikaConfiguration = tikaConfiguration;
        this.executor = Executor.newInstance();
        this.uri = buildURI(tikaConfiguration);
    }

    private URI buildURI(TikaConfiguration tikaConfiguration) throws URISyntaxException {
        return new URIBuilder()
                .setHost(tikaConfiguration.getHost())
                .setPort(tikaConfiguration.getPort())
                .setScheme("http")
                .build();
    }

    @Override
    public String metadataAndContent(InputStream inputStream, String contentType) throws TikaException {
        return executePut(uri.resolve(RMETA_AS_TEXT_ENDPOINT), inputStream, contentType);
    }

    private String executePut(URI uri, InputStream inputStream, String contentType) throws TikaException {
        try {
            return executor.execute(Request.Put(uri)
                        .addHeader("Content-type", contentType)
                        .socketTimeout(tikaConfiguration.getTimeoutInMillis())
                        .bodyStream(inputStream))
                    .returnContent()
                    .asString(Charsets.UTF_8);
        } catch (IOException e) {
            throw new TikaException(e);
        }
    }

}
