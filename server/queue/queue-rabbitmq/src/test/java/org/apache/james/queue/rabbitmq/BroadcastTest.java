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

package org.apache.james.queue.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.steveash.guavate.Guavate;
import com.google.common.collect.ImmutableList;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@ExtendWith(DockerRabbitMQExtension.class)
public class BroadcastTest {

    private static final String EXCHANGE_NAME = "exchangeName";
    private static final boolean DURABLE = true;
    private static final String ROUTING_KEY = "";
    private static final AMQP.BasicProperties NO_PROPERTIES = null;
    private static final long ONE_MINUTE = TimeUnit.MINUTES.toMillis(1);

    private ConnectionFactory connectionFactory1;
    private ConnectionFactory connectionFactory2;
    private ConnectionFactory connectionFactory3;
    private ConnectionFactory connectionFactory4;

    @BeforeEach
    public void setup(DockerRabbitMQ rabbitMQ) {
        connectionFactory1 = prepareConnectionFactory(rabbitMQ);
        connectionFactory2 = prepareConnectionFactory(rabbitMQ);
        connectionFactory3 = prepareConnectionFactory(rabbitMQ);
        connectionFactory4 = prepareConnectionFactory(rabbitMQ);
    }

    private ConnectionFactory prepareConnectionFactory(DockerRabbitMQ rabbitMQ) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitMQ.getHostIp());
        connectionFactory.setPort(rabbitMQ.getPort());
        connectionFactory.setUsername(rabbitMQ.getUsername());
        connectionFactory.setPassword(rabbitMQ.getPassword());
        return connectionFactory;
    }

    @Test
    // In the following case, each consumer will receive the messages produced by the
    // producer

    // To do so, each consumer will bind it's queue to the producer exchange.
    public void rabbitMQShouldSupportTheBroadcastCase() throws Exception {
        ConcurrentLinkedQueue<Integer> results2 = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Integer> results3 = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Integer> results4 = new ConcurrentLinkedQueue<>();

        try (Connection connection1 = connectionFactory1.newConnection();
             Channel publisherChannel = connection1.createChannel();
            Connection connection2 = connectionFactory2.newConnection();
             Channel subscriberChannel2 = connection2.createChannel();
            Connection connection3 = connectionFactory3.newConnection();
             Channel subscriberChannel3 = connection3.createChannel();
            Connection connection4 = connectionFactory4.newConnection();
             Channel subscriberChannel4 = connection4.createChannel()) {

            // Declare the a single exchange and three queues attached to it.
            publisherChannel.exchangeDeclare(EXCHANGE_NAME, "direct", DURABLE);

            String queue2 = subscriberChannel2.queueDeclare().getQueue();
            subscriberChannel2.queueBind(queue2, EXCHANGE_NAME, ROUTING_KEY);
            String queue3 = subscriberChannel3.queueDeclare().getQueue();
            subscriberChannel3.queueBind(queue3, EXCHANGE_NAME, ROUTING_KEY);
            String queue4 = subscriberChannel4.queueDeclare().getQueue();
            subscriberChannel4.queueBind(queue4, EXCHANGE_NAME, ROUTING_KEY);

            subscriberChannel2.basicConsume(queue2, storeInResultCallBack(subscriberChannel2, results2));
            subscriberChannel3.basicConsume(queue3, storeInResultCallBack(subscriberChannel3, results3));
            subscriberChannel4.basicConsume(queue4, storeInResultCallBack(subscriberChannel4, results4));

            // the publisher will produce 10 messages
            for (int i = 0; i < 10; i++) {
                publisherChannel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, NO_PROPERTIES,
                    String.valueOf(i).getBytes(StandardCharsets.UTF_8));
            }

            Thread.sleep(ONE_MINUTE);

            // Check every subscriber have receive all the messages.
            ImmutableList<Integer> expectedResult = IntStream.range(0, 10).boxed().collect(Guavate.toImmutableList());
            assertThat(results2).containsOnlyElementsOf(expectedResult);
            assertThat(results3).containsOnlyElementsOf(expectedResult);
            assertThat(results4).containsOnlyElementsOf(expectedResult);
        }
    }

    private DefaultConsumer storeInResultCallBack(Channel channel, ConcurrentLinkedQueue<Integer> results) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                Integer payload = Integer.valueOf(new String(body, StandardCharsets.UTF_8));
                results.add(payload);
            }
        };
    }
}
