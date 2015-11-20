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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.james.mailbox.store.mail.model.MailboxId;
import org.apache.james.mailbox.store.mail.model.Message;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

public class SortToComparatorConvertor<Id extends MailboxId> {

    private static final String SEPARATOR = " ";
    private static final String DESC_ORDERING = "desc";

    private SortToComparatorConvertor() {
    }

    @SuppressWarnings("rawtypes")
    private static final Map<String, Function<Message<?>, Comparable>> fieldsMessageFunctionMap = ImmutableMap.of(
            "date", Message::getInternalDate,
            "id", Message::getUid);

    public static <Id extends MailboxId> Comparator<Message<Id>> comparatorFor(List<String> sort) {
        ComparatorBuilder<Message<Id>> comparator = new ComparatorBuilder<Message<Id>>();
        sort.stream()
            .forEach(field -> comparatorForField(field, comparator));
        return comparator.getComparator();
    }

    @SuppressWarnings("unchecked")
    private static <Id extends MailboxId> void comparatorForField(String field, ComparatorBuilder<Message<Id>> comparator) {
        List<String> splitToList = Splitter.on(SEPARATOR).splitToList(field);
        Comparator<Message<Id>> fieldComparator = Comparator.comparing(fieldsMessageFunctionMap.get(splitToList.get(0)));
        if (splitToList.size() == 1 || splitToList.get(1).equals(DESC_ORDERING)) {
            comparator.thenComparing(fieldComparator.reversed());
        } else {
            comparator.thenComparing(fieldComparator);
        }
    }

    private static class ComparatorBuilder<Type> {

        private Comparator<Type> comparator;

        public ComparatorBuilder() {
            comparator = new EmptyComparator<Type>();
        }

        public void thenComparing(Comparator<Type> otherComparator) {
            comparator = comparator.thenComparing(otherComparator);
        }

        public Comparator<Type> getComparator() {
            return comparator;
        }
    }

    private static class EmptyComparator<Type> implements Comparator<Type> {

        @Override
        public int compare(Type o1, Type o2) {
            return 0;
        }
        
    }
}
