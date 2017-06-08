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

package org.apache.james.imap.processor.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;

public class UidMsnConverterTest {
    private UidMsnConverter testee;
    private MessageUid messageUid1;
    private MessageUid messageUid2;
    private MessageUid messageUid3;
    private MessageUid messageUid4;

    @Before
    public void setUp() {
        testee = new UidMsnConverter(ImmutableList.<MessageUid>of().iterator());
        messageUid1 = MessageUid.of(1);
        messageUid2 = MessageUid.of(2);
        messageUid3 = MessageUid.of(3);
        messageUid4 = MessageUid.of(4);
    }

    @Test
    public void getUidShouldReturnEmptyIfNoMessageWithTheGivenMessageNumber() {
        assertThat(testee.getUid(1))
            .isAbsent();
    }

    @Test
    public void getUidShouldReturnEmptyIfZero() {
        assertThat(testee.getUid(0))
            .isAbsent();
    }

    @Test
    public void getUidShouldTheCorrespondingUidIfItExist() {
        testee.addUid(messageUid1);

        assertThat(testee.getUid(1))
            .contains(messageUid1);
    }

    @Test
    public void getFirstUidShouldReturnEmptyIfNoMessage() {
        assertThat(testee.getFirstUid()).isAbsent();
    }

    @Test
    public void getLastUidShouldReturnEmptyIfNoMessage() {
        assertThat(testee.getLastUid()).isAbsent();
    }

    @Test
    public void getFirstUidShouldReturnFirstUidIfAtLeastOneMessage() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getFirstUid()).contains(messageUid1);
    }

    @Test
    public void getLastUidShouldReturnLastUidIfAtLeastOneMessage() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getLastUid()).contains(messageUid2);
    }

    @Test
    public void getMsnShouldReturnAbsentIfNoCorrespondingMessage() {
        testee.addUid(messageUid1);

        assertThat(testee.getMsn(messageUid2)).isAbsent();
    }

    @Test
    public void getMsnShouldReturnMessageNumberIfUidIsThere() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getMsn(messageUid2))
            .contains(2);
    }

    @Test
    public void getNumMessageShouldReturnZeroIfNoMapping() {
        assertThat(testee.getNumMessage())
            .isEqualTo(0);
    }

    @Test
    public void getNumMessageShouldReturnTheNumOfMessage() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);

        assertThat(testee.getNumMessage())
            .isEqualTo(2);
    }

    @Test
    public void isEmptyShouldReturnTrueIfNoMapping() {
        assertThat(testee.isEmpty())
            .isTrue();
    }

    @Test
    public void isEmptyShouldReturnFalseIfMapping() {
        testee.addUid(messageUid1);

        assertThat(testee.isEmpty())
            .isFalse();
    }

    @Test
    public void clearShouldClearMapping() {
        testee.addUid(messageUid1);

        testee.clear();

        assertThat(testee.isEmpty())
            .isTrue();
    }

    @Test
    public void addUidShouldKeepMessageNumberContiguous() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);
        testee.addUid(messageUid2);

        assertThat(testee.getConversion())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid3,
                4, messageUid4));
    }

    @Test
    public void addUidShouldNotOverridePreviousMapping() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid2);

        assertThat(testee.getMsn(messageUid2))
            .contains(2);
    }

    @Test
    public void removeShouldKeepAValidMappingWhenDeletingBeginning() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);

        testee.remove(messageUid1);

        assertThat(testee.getConversion())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid2,
                2, messageUid3,
                3, messageUid4));
    }

    @Test
    public void removeShouldKeepAValidMappingWhenDeletingEnd() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);

        testee.remove(messageUid4);

        assertThat(testee.getConversion())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid3));
    }

    @Test
    public void removeShouldKeepAValidMappingWhenDeletingMiddle() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);

        testee.remove(messageUid3);

        assertThat(testee.getConversion())
            .isEqualTo(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid4));
    }

    @Test
    public void addUidShouldSupportOutOfOrderUpdates() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid3);
        testee.addUid(messageUid2);
        testee.addUid(messageUid4);

        assertThat(testee.getConversion().entrySet())
            .containsOnlyElementsOf(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid3,
                4, messageUid4).entrySet());
    }

    @Test
    public void addUidShouldLeadToValidConvertionWhenInsertInFirstPosition() {
        testee.addUid(messageUid2);
        testee.addUid(messageUid3);
        testee.addUid(messageUid4);
        testee.addUid(messageUid1);

        assertThat(testee.getConversion().entrySet())
            .containsOnlyElementsOf(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid3,
                4, messageUid4).entrySet());
    }

    @Test
    public void constructorWithOutOfOrderIteratorShouldLeadToValidConvertion() {
        testee = new UidMsnConverter(ImmutableList.of(messageUid2,
            messageUid3,
            messageUid4,
            messageUid1)
            .iterator());

        assertThat(testee.getConversion().entrySet())
            .containsOnlyElementsOf(ImmutableBiMap.of(
                1, messageUid1,
                2, messageUid2,
                3, messageUid3,
                4, messageUid4).entrySet());
    }

    @Test
    public void addUidShouldBeIdempotent() {
        testee.addUid(messageUid1);
        testee.addUid(messageUid1);

        assertThat(testee.getConversion())
            .isEqualTo(ImmutableBiMap.of(1, messageUid1));
    }

    @Test
    public void addAndRemoveShouldLeadToValidConvertionWhenMixed() throws Exception {
        final int initialCount = 1000;
        for (int i = 1; i <= initialCount; i++) {
            testee.addUid(MessageUid.of(i));
        }

        int threadCount = 2;
        ConcurrentTestRunner concurrentTestRunner = new ConcurrentTestRunner(threadCount, initialCount,
            new ConcurrentTestRunner.BiConsumer() {
                @Override
                public void consume(int threadNumber, int step) throws Exception {
                    if (threadNumber == 0) {
                        testee.remove(MessageUid.of(step + 1));
                    } else {
                        testee.addUid(MessageUid.of(initialCount + step + 1));
                    }
                }
            });
        concurrentTestRunner.run();
        concurrentTestRunner.awaitTermination(10, TimeUnit.SECONDS);

        ImmutableBiMap.Builder<Integer, MessageUid> resultBuilder = ImmutableBiMap.builder();
        for (int i = 1; i <= initialCount; i++) {
            resultBuilder.put(i, MessageUid.of(initialCount + i));
        }
        assertThat(testee.getConversion().entrySet())
            .containsOnlyElementsOf(resultBuilder.build().entrySet());
    }

    @Test
    public void addShouldLeadToValidConvertionWhenConcurrent() throws Exception {
        final int operationCount = 1000;
        int threadCount = 2;

        ConcurrentTestRunner concurrentTestRunner = new ConcurrentTestRunner(threadCount, operationCount,
            new ConcurrentTestRunner.BiConsumer() {
                @Override
                public void consume(int threadNumber, int step) throws Exception {
                    testee.addUid(MessageUid.of((threadNumber * operationCount) + (step + 1)));
                }
            });
        concurrentTestRunner.run();
        concurrentTestRunner.awaitTermination(10, TimeUnit.SECONDS);

        ImmutableBiMap.Builder<Integer, MessageUid> resultBuilder = ImmutableBiMap.builder();
        for (int i = 1; i <= threadCount * operationCount; i++) {
            resultBuilder.put(i, MessageUid.of(i));
        }
        assertThat(testee.getConversion().entrySet())
            .containsOnlyElementsOf(resultBuilder.build().entrySet());
    }

    @Test
    public void removeShouldLeadToValidConvertionWhenConcurrent() throws Exception {
        final int operationCount = 1000;
        int threadCount = 2;
        for (int i = 1; i <= operationCount * (threadCount + 1); i++) {
            testee.addUid(MessageUid.of(i));
        }

        ConcurrentTestRunner concurrentTestRunner = new ConcurrentTestRunner(threadCount, operationCount,
            new ConcurrentTestRunner.BiConsumer() {
                @Override
                public void consume(int threadNumber, int step) throws Exception {
                    testee.remove(MessageUid.of((threadNumber * operationCount) + (step + 1)));
                }
            });
        concurrentTestRunner.run();
        concurrentTestRunner.awaitTermination(10, TimeUnit.SECONDS);

        ImmutableBiMap.Builder<Integer, MessageUid> resultBuilder = ImmutableBiMap.builder();
        for (int i = 1; i <= operationCount; i++) {
            resultBuilder.put(i, MessageUid.of((threadCount * operationCount) + i));
        }
        assertThat(testee.getConversion().entrySet())
            .containsOnlyElementsOf(resultBuilder.build().entrySet());
    }

}