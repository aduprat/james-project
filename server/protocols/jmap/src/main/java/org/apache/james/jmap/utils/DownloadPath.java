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

package org.apache.james.jmap.utils;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class DownloadPath {

    public static DownloadPath from(String path) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "'path' is mandatory");

        List<String> pathVariables = Splitter.on('/').omitEmptyStrings().splitToList(path);
        Preconditions.checkArgument(pathVariables.size() >= 1, "'blobId' is mandatory");

        return new DownloadPath(pathVariables.get(0), name(pathVariables));
    }

    private static Optional<String> name(List<String> pathVariables) {
        return pathVariables.size() >= 2 ? Optional.of(pathVariables.get(1)) : Optional.empty();
    }

    private final String blobId;
    private final Optional<String> name;

    private DownloadPath(String blobId, Optional<String> name) {
        this.blobId = blobId;
        this.name = name;
    }

    public String getBlobId() {
        return blobId;
    }

    public Optional<String> getName() {
        return name;
    }
}
