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

import javax.mail.Flags.Flag;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.model.Filter;
import org.apache.james.jmap.model.FilterCondition;
import org.apache.james.jmap.model.FilterOperator;
import org.apache.james.mailbox.model.SearchQuery;
import org.apache.james.mailbox.model.SearchQuery.AddressType;
import org.apache.james.mailbox.model.SearchQuery.Criterion;
import org.apache.james.mailbox.model.SearchQuery.DateResolution;

import com.github.fge.lambdas.Throwing;
import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;

public class FilterToSearchQuery {

    public SearchQuery convert(Filter filter) {
        if (filter instanceof FilterCondition) {
            return convertCondition((FilterCondition) filter);
        }
        if (filter instanceof FilterOperator) {
            return convertOperator((FilterOperator) filter);
        }
        throw new RuntimeException("Unknown filter");
    }

    private SearchQuery convertCondition(FilterCondition filter) {
        SearchQuery searchQuery = new SearchQuery();
        filter.getText().ifPresent(text -> {
            searchQuery.andCriteria(
                    SearchQuery.or(ImmutableList.of(
                            SearchQuery.address(AddressType.From, text),
                            SearchQuery.address(AddressType.To, text),
                            SearchQuery.address(AddressType.Cc, text),
                            SearchQuery.address(AddressType.Bcc, text),
                            SearchQuery.headerContains("Subject", text),
                            SearchQuery.bodyContains(text)))
                    );
        });
        filter.getFrom().ifPresent(from -> searchQuery.andCriteria(SearchQuery.address(AddressType.From, from)));
        filter.getTo().ifPresent(to -> searchQuery.andCriteria(SearchQuery.address(AddressType.To, to)));
        filter.getCc().ifPresent(cc -> searchQuery.andCriteria(SearchQuery.address(AddressType.Cc, cc)));
        filter.getBcc().ifPresent(bcc -> searchQuery.andCriteria(SearchQuery.address(AddressType.Bcc, bcc)));
        filter.getSubject().ifPresent(subject -> searchQuery.andCriteria(SearchQuery.headerContains("Subject", subject)));
        filter.getBody().ifPresent(body ->  searchQuery.andCriteria(SearchQuery.bodyContains(body)));
        filter.getAfter().ifPresent(after -> searchQuery.andCriteria(SearchQuery.internalDateAfter(after, DateResolution.Second)));
        filter.getBefore().ifPresent(before -> searchQuery.andCriteria(SearchQuery.internalDateBefore(before, DateResolution.Second)));
        filter.getHasAttachment().ifPresent(Throwing.consumer(hasAttachment -> { throw new NotImplementedException(); } ));
        filter.getHeader().ifPresent(header -> searchQuery.andCriteria(SearchQuery.headerContains(header.getName(), header.getValue().orElse(null))));
        filter.getIsAnswered().ifPresent(isAnswered -> searchQuery.andCriteria(SearchQuery.flagIsSet(Flag.ANSWERED)));
        filter.getIsDraft().ifPresent(isDraft -> searchQuery.andCriteria(SearchQuery.flagIsSet(Flag.DRAFT)));
        filter.getIsFlagged().ifPresent(isFlagged -> searchQuery.andCriteria(SearchQuery.flagIsSet(Flag.FLAGGED)));
        filter.getIsUnread().ifPresent(isUnread -> searchQuery.andCriteria(SearchQuery.flagIsUnSet(Flag.SEEN)));
        filter.getMaxSize().ifPresent(maxSize -> searchQuery.andCriteria(SearchQuery.sizeLessThan(maxSize)));
        filter.getMinSize().ifPresent(minSize -> searchQuery.andCriteria(SearchQuery.sizeGreaterThan(minSize)));
        return searchQuery;
    }

    private SearchQuery convertOperator(FilterOperator filter) {
        SearchQuery searchQuery = new SearchQuery();
        switch (filter.getOperator()) {
        case AND:
            searchQuery.andCriteria(SearchQuery.and(convertCriterias(filter)));
            return searchQuery;
   
        case OR:
            searchQuery.andCriteria(SearchQuery.or(convertCriterias(filter)));
            return searchQuery;
   
        case NOT:
            searchQuery.andCriteria(SearchQuery.not(convertCriterias(filter)));
            return searchQuery;
        }
        throw new RuntimeException("Unknown operator");
    }

    private ImmutableList<Criterion> convertCriterias(FilterOperator filter) {
        return filter.getConditions().stream()
            .map(this::convert)
            .flatMap(sq -> sq.getCriterias().stream())
            .collect(Guavate.toImmutableList());
    }
}
