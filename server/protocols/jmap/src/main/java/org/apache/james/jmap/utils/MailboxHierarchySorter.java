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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.james.jmap.model.mailbox.Mailbox;

import com.google.common.collect.Lists;

public class MailboxHierarchySorter<T, Id> {

    private final Function<T, Id> index;
    private final Function<T, Optional<Id>> parentId;

    public MailboxHierarchySorter(Function<T, Id> index,
                                  Function<T, Optional<Id>> parentId) {
        this.index = index;
        this.parentId = parentId;
    }

    public List<T> sortFromRootToLeaf(Collection<T> mailboxes) {

        Map<Id, T> mapOfMailboxesById = mailboxes.stream()
                .collect(Collectors.toMap(index, Function.identity()));

        DependencyGraph<T> graph = new DependencyGraph<>(m ->
                parentId.apply(m).map(mapOfMailboxesById::get));

        mailboxes.stream().forEach(graph::registerItem);

        return graph.getBuildChain().collect(Collectors.toList());
    }

    public List<T> sortFromLeafToRoot(Collection<T> mailboxes) {
        return Lists.reverse(sortFromRootToLeaf(mailboxes));
    }
}
