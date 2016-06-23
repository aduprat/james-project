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

package org.apache.james.mailbox.store.mail.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class Attachment {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private AttachmentId attachmentId;
        private byte[] bytes;
        private String type;
        private Optional<String> name;
        private Long size;

        private Builder() {
            name = Optional.absent();
        }

        public Builder attachmentId(AttachmentId attachmentId) {
            Preconditions.checkArgument(attachmentId != null);
            this.attachmentId = attachmentId;
            return this;
        }

        public Builder bytes(byte[] bytes) {
            Preconditions.checkArgument(bytes != null);
            this.bytes = bytes;
            return this;
        }

        public Builder type(String type) {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(type));
            this.type = type;
            return this;
        }

        public Builder name(Optional<String> name) {
            Preconditions.checkArgument(name != null);
            this.name = name;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Attachment build() {
            Preconditions.checkState(bytes != null, "'bytes' is mandatory");
            AttachmentId builtAttachmentId = attachmentId();
            Long builtSize = size();
            Preconditions.checkState(builtAttachmentId != null, "'attachmentId' is mandatory");
            Preconditions.checkState(type != null, "'type' is mandatory");
            Preconditions.checkState(builtSize != null, "'size' is mandatory");
            return new Attachment(bytes, builtAttachmentId, type, name, builtSize);
        }

        private AttachmentId attachmentId() {
            return MoreObjects.firstNonNull(attachmentId, AttachmentId.forPayload(bytes));
        }

        private Long size() {
            return MoreObjects.firstNonNull(size, Long.valueOf(bytes.length));
        }
    }

    private final byte[] bytes;
    private final AttachmentId attachmentId;
    private final String type;
    private final Optional<String> name;
    private final long size;

    private Attachment(byte[] bytes, AttachmentId attachmentId, String type, Optional<String> name, long size) {
        this.bytes = bytes;
        this.attachmentId = attachmentId;
        this.type = type;
        this.name = name;
        this.size = size;
    }

    public AttachmentId getAttachmentId() {
        return attachmentId;
    }

    public String getType() {
        return type;
    }

    public Optional<String> getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public InputStream getStream() throws IOException {
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Attachment) {
            Attachment other = (Attachment) obj;
            return Objects.equal(attachmentId, other.attachmentId)
                && Arrays.equals(bytes, other.bytes)
                && Objects.equal(type, other.type)
                && Objects.equal(name, other.name)
                && Objects.equal(size, other.size);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(attachmentId, bytes, type, name, size);
    }

    @Override
    public String toString() {
        return MoreObjects
                .toStringHelper(this)
                .add("attachmentId", attachmentId)
                .add("bytes", bytes)
                .add("type", type)
                .add("name", name)
                .add("size", size)
                .toString();
    }
}
