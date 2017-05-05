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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.mailbox.extractor.ParsedContent;
import org.apache.james.mailbox.extractor.TextExtractor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class TikaTextExtractor implements TextExtractor {

    private final TikaHttpClient tikaHttpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public TikaTextExtractor(TikaHttpClient tikaHttpClient) {
        this.tikaHttpClient = tikaHttpClient;
        this.objectMapper = initializeObjectMapper();
    }

    private ObjectMapper initializeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule mapModule = new SimpleModule();
        mapModule.addDeserializer(ContentAndMetadata.class, new ContentAndMetadataDeserializer());
        objectMapper.registerModule(mapModule);
        return objectMapper;
    }

    @Override
    public ParsedContent extractContent(InputStream inputStream, String contentType) throws Exception {
        ContentAndMetadata contentAndMetadata = convert(tikaHttpClient.rmetaAsJson(inputStream, contentType));
        return new ParsedContent(contentAndMetadata.getContent(), contentAndMetadata.getMetadata());
    }

    private ContentAndMetadata convert(InputStream json) throws IOException, JsonParseException, JsonMappingException {
        return objectMapper.readValue(json, ContentAndMetadata.class);
    }

    @VisibleForTesting static class ContentAndMetadataDeserializer extends JsonDeserializer<ContentAndMetadata> {

        @Override
        public ContentAndMetadata deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            TreeNode treeNode = jsonParser.getCodec().readTree(jsonParser);
            Preconditions.checkState(treeNode.isArray() && treeNode.size() == 1, "The response should have only one element");
            Preconditions.checkState(treeNode.get(0).isObject(), "The element should be a Json object");
            ObjectNode node = (ObjectNode) treeNode.get(0);
            return ContentAndMetadata.from(ImmutableList.copyOf(node.fields()).stream()
                .map(entry -> Pair.of(entry.getKey(), asListOfString(entry.getValue())))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
        }

        @VisibleForTesting List<String> asListOfString(JsonNode jsonNode) {
            if (jsonNode.isArray()) {
                return ImmutableList.copyOf(jsonNode.elements()).stream()
                    .map(node -> node.asText())
                    .collect(Collectors.toList());
            }
            return ImmutableList.of(jsonNode.asText());
        }
        
    }
    
    private static class ContentAndMetadata {

        private static final String TIKA_HEADER = "X-TIKA";
        private static final String CONTENT_METADATA_HEADER_NAME = TIKA_HEADER + ":content";

        public static ContentAndMetadata from(Map<String, List<String>> contentAndMetadataMap) {
            return new ContentAndMetadata(content(contentAndMetadataMap),
                    contentAndMetadataMap.entrySet().stream()
                        .filter(entry -> { return !entry.getKey().startsWith(TIKA_HEADER); })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }

        private static String content(Map<String, List<String>> contentAndMetadataMap) {
            List<String> content = contentAndMetadataMap.get(CONTENT_METADATA_HEADER_NAME);
            if (content == null) {
                return null;
            }
            String onlySpaces = null;
            return StringUtils.stripStart(content.get(0), onlySpaces);
        }

        private final String content;
        private final Map<String, List<String>> metadata;

        private ContentAndMetadata(String content, Map<String, List<String>> metadata) {
            this.content = content;
            this.metadata = metadata;
        }

        public String getContent() {
            return content;
        }

        public Map<String, List<String>> getMetadata() {
            return metadata;
        }

        @Override
        public final boolean equals(Object o) {
            if (o instanceof ContentAndMetadata) {
                ContentAndMetadata other = (ContentAndMetadata) o;
                return Objects.equals(content, other.content)
                    && Objects.equals(metadata, other.metadata);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return Objects.hash(content, metadata);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("content", content)
                .add("metadata", metadata)
                .toString();
        }
    }
}
