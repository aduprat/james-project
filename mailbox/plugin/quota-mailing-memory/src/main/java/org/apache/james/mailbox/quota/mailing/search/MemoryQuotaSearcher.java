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
package org.apache.james.mailbox.quota.mailing.search;

import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.apache.james.core.User;
import org.apache.james.eventsourcing.eventstore.memory.InMemoryEventStore;
import org.apache.james.mailbox.quota.mailing.aggregates.UserQuotaThresholds;
import org.apache.james.mailbox.quota.mailing.aggregates.UserQuotaThresholds.Id;
import org.apache.james.mailbox.quota.mailing.search.QuotaQuery.LessThanQuery;
import org.apache.james.mailbox.quota.mailing.search.QuotaQuery.MoreThanQuery;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.Lists;

public class MemoryQuotaSearcher implements QuotaSearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemoryQuotaSearcher.class);

    private final UsersRepository usersRepository;
    private final InMemoryEventStore store;

    @Inject
    public MemoryQuotaSearcher(UsersRepository usersRepository, InMemoryEventStore store) {
        this.usersRepository = usersRepository;
        this.store = store;
    }

    @Override
    public List<User> search(QuotaQuery query, int limit, int offset) {
        try {
            return Lists.newArrayList(usersRepository.list()).stream()
                .map(User::fromUsername)
                .map(Id::from)
                .map(id -> UserQuotaThresholds.fromEvents(id, store.getEventsOfAggregate(id)))
                .filter(userQuotaThresholds -> filter(query, userQuotaThresholds))
                .map(UserQuotaThresholds::getId)
                .map(Id::getUser)
                .sorted(Comparator.comparing(User::asString))
                .limit(limit)
                .collect(Guavate.toImmutableList());
        } catch (UsersRepositoryException e) {
            LOGGER.warn("Error while listing users", e);
            return Lists.newArrayList();
        }
    }

    private boolean filter(QuotaQuery query, UserQuotaThresholds userQuotaThresholds) {
        if (query instanceof MoreThanQuery) {
            return userQuotaThresholds.hasReached(((MoreThanQuery) query).getQuotaThreshold());
        }
        if (query instanceof LessThanQuery) {
            return !userQuotaThresholds.hasReached(((LessThanQuery) query).getQuotaThreshold());
        }
        return true;
    }

}
