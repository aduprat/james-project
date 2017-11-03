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
package org.apache.james.mailbox;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;

public class PathDelimiter {

    private final Joiner joiner;
    private final Splitter splitter;
    private final char charDelimiter;

    public PathDelimiter(char charDelimiter) {
        this.charDelimiter = charDelimiter;
        this.joiner = Joiner.on(charDelimiter);
        this.splitter = Splitter.on(charDelimiter);
    }
    
    public String join(String... paths) {
        return joiner.join(paths);
    }

    public String join(List<String> paths) {
        return joiner.join(paths);
    }

    public List<String> split(String path) {
        return splitter.splitToList(path);
    }
   
    /**
     * @deprecated Will be removed, prefere to use {@link PathDelimiter#join} or {@link PathDelimiter#split}
     */
    @Deprecated
    public char getPathDelimiter() {
        return charDelimiter;
    }
    
    @Override
    public final int hashCode() {
        return Objects.hashCode(charDelimiter);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof PathDelimiter) {
            PathDelimiter other = (PathDelimiter) obj;
            return this.charDelimiter == other.charDelimiter;
        }
        return false;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
            .add("charDelimiter", charDelimiter)
            .toString();
    }
}
