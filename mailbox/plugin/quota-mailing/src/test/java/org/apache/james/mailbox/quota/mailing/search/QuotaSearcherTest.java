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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.james.core.Domain;
import org.apache.james.core.User;
import org.apache.james.eventsourcing.EventId;
import org.apache.james.eventsourcing.eventstore.EventStore;
import org.apache.james.mailbox.model.Quota;
import org.apache.james.mailbox.quota.QuotaCount;
import org.apache.james.mailbox.quota.QuotaSize;
import org.apache.james.mailbox.quota.mailing.aggregates.UserQuotaThresholds;
import org.apache.james.mailbox.quota.mailing.events.QuotaThresholdChangedEvent;
import org.apache.james.mailbox.quota.mailing.search.QuotaQuery.AndQuery;
import org.apache.james.mailbox.quota.mailing.search.QuotaQuery.HasDomainQuery;
import org.apache.james.mailbox.quota.mailing.search.QuotaQuery.LessThanQuery;
import org.apache.james.mailbox.quota.mailing.search.QuotaQuery.MoreThanQuery;
import org.apache.james.mailbox.quota.model.HistoryEvolution;
import org.apache.james.mailbox.quota.model.QuotaThreshold;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public interface QuotaSearcherTest {

    String DOMAIN = "bar.com";
    String DOMAIN2 = "JoeBarTeam.com";
    User USER = User.from("foo", Optional.of(DOMAIN));
    User USER2 = User.from("foo2", Optional.of(DOMAIN));
    User USER3 = User.from("foo3", Optional.of(DOMAIN));
    User USER4 = User.from("foo", Optional.of(DOMAIN2));
    User USER5 = User.from("foo", Optional.of(DOMAIN));
    Quota<QuotaSize> _1_ON_3_SIZE_QUOTA = Quota.<QuotaSize>builder().used(QuotaSize.size(10)).computedLimit(QuotaSize.size(30)).build();
    Quota<QuotaSize> _2_ON_3_SIZE_QUOTA = Quota.<QuotaSize>builder().used(QuotaSize.size(20)).computedLimit(QuotaSize.size(30)).build();
    Quota<QuotaCount> _1_ON_4_COUNT_QUOTA = Quota.<QuotaCount>builder().used(QuotaCount.count(1)).computedLimit(QuotaCount.count(4)).build();
    Quota<QuotaCount> _2_ON_4_COUNT_QUOTA = Quota.<QuotaCount>builder().used(QuotaCount.count(2)).computedLimit(QuotaCount.count(4)).build();
    Quota<QuotaCount> _3_ON_4_COUNT_QUOTA = Quota.<QuotaCount>builder().used(QuotaCount.count(3)).computedLimit(QuotaCount.count(4)).build();
    Instant INSTANT = Instant.ofEpochMilli(45554);
    QuotaThresholdChangedEvent _1_ON_3_SIZE_EVENT = new QuotaThresholdChangedEvent(
            EventId.first(),
            HistoryEvolution.noChanges(),
            HistoryEvolution.noChanges(),
            _1_ON_3_SIZE_QUOTA,
            _1_ON_4_COUNT_QUOTA,
            UserQuotaThresholds.Id.from(USER));
    QuotaThresholdChangedEvent _2_ON_3_SIZE_EVENT = new QuotaThresholdChangedEvent(
            EventId.first(),
            HistoryEvolution.noChanges(),
            HistoryEvolution.noChanges(),
            _2_ON_3_SIZE_QUOTA,
            _1_ON_4_COUNT_QUOTA,
            UserQuotaThresholds.Id.from(USER2));
    QuotaThresholdChangedEvent _2_ON_4_COUNT_EVENT = new QuotaThresholdChangedEvent(
            EventId.first(),
            HistoryEvolution.noChanges(),
            HistoryEvolution.noChanges(),
            _1_ON_3_SIZE_QUOTA,
            _2_ON_4_COUNT_QUOTA,
            UserQuotaThresholds.Id.from(USER3));
    QuotaThresholdChangedEvent _3_ON_4_COUNT_EVENT = new QuotaThresholdChangedEvent(
            EventId.first(),
            HistoryEvolution.noChanges(),
            HistoryEvolution.noChanges(),
            _1_ON_3_SIZE_QUOTA,
            _3_ON_4_COUNT_QUOTA,
            UserQuotaThresholds.Id.from(USER4));
    QuotaThresholdChangedEvent _2_ON_3_SIZE_3_ON_4_COUNT_EVENT = new QuotaThresholdChangedEvent(
            EventId.first(),
            HistoryEvolution.noChanges(),
            HistoryEvolution.noChanges(),
            _2_ON_3_SIZE_QUOTA,
            _3_ON_4_COUNT_QUOTA,
            UserQuotaThresholds.Id.from(USER5));

    QuotaSearcher quotaSearcher();
    EventStore eventStore();

    @BeforeAll
    default void setup(EventStore eventStore) {
        eventStore.append(_1_ON_3_SIZE_EVENT);
        eventStore.append(_2_ON_3_SIZE_EVENT);
        eventStore.append(_2_ON_4_COUNT_EVENT);
        eventStore.append(_3_ON_4_COUNT_EVENT);
        eventStore.append(_2_ON_3_SIZE_3_ON_4_COUNT_EVENT);
    }

    @Test
    default void searchShouldReturnFilteredUsersWhenLessThanQuery() {
        int limit = 10;
        int offset = 0;
        List<User> moreThan = quotaSearcher().search(new LessThanQuery(new QuotaThreshold(2/3)), limit, offset);

        assertThat(moreThan).contains(USER, USER2);
    }

    @Test
    default void searchShouldReturnFilteredUsersWhenMoreThanQuery() {
        int limit = 10;
        int offset = 0;
        List<User> moreThan = quotaSearcher().search(new MoreThanQuery(new QuotaThreshold(2/3)), limit, offset);

        assertThat(moreThan).contains(USER3, USER4, USER5);
    }

    @Test
    default void searchShouldReturnALimitedNumberOfUsersWhenLimitIsReached() {
        int limit = 2;
        int offset = 0;
        List<User> moreThan = quotaSearcher().search(new MoreThanQuery(new QuotaThreshold(2/3)), limit, offset);

        assertThat(moreThan).contains(USER3, USER4);
    }

    @Test
    default void searchShouldReturnALimitedNumberOfUsersWhenLimitIsReachedAndOffsetIsGiven() {
        int limit = 2;
        int offset = 1;
        List<User> moreThan = quotaSearcher().search(new MoreThanQuery(new QuotaThreshold(2/3)), limit, offset);

        assertThat(moreThan).contains(USER4, USER5);
    }

    @Test
    default void searchShouldReturnFilteredUsersWhenDomainIsGiven() {
        int limit = 10;
        int offset = 0;
        List<User> moreThan = quotaSearcher().search(new HasDomainQuery(Domain.of(DOMAIN2)), limit, offset);

        assertThat(moreThan).contains(USER4);
    }

    @Test
    default void searchShouldReturnFilteredUsersWhenAndQuery() {
        int limit = 10;
        int offset = 0;
        List<User> moreThan = quotaSearcher().search(new AndQuery(new MoreThanQuery(new QuotaThreshold(2/3)), new HasDomainQuery(Domain.of(DOMAIN))), limit, offset);

        assertThat(moreThan).contains(USER3, USER5);
    }

    @Test
    default void searchShouldReturnFilteredUsersWhenOrQuery() {
        int limit = 10;
        int offset = 0;
        List<User> moreThan = quotaSearcher().search(new AndQuery(new LessThanQuery(new QuotaThreshold(2/3)), new HasDomainQuery(Domain.of(DOMAIN))), limit, offset);

        assertThat(moreThan).contains(USER, USER2, USER4);
    }
}
