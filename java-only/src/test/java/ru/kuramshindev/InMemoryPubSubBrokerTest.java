package ru.kuramshindev;

import org.junit.jupiter.api.Test;
import ru.kuramshindev.pubsubbroker.InMemoryPubSubBroker;
import ru.kuramshindev.pubsubbroker.PubSubBroker;
import ru.kuramshindev.pubsubbroker.Subscription;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryPubSubBrokerTest {

    @Test
    void shouldDeliverMessageToSubscriber() {

        PubSubBroker broker = new InMemoryPubSubBroker();

        List<String> received = new ArrayList<>();

        broker.subscribe("orders", received::add);

        broker.publish("orders", "order-created");

        assertEquals(List.of("order-created"), received);

    }

    @Test
    void shouldDeliverMessageToAllSubscribersOfTopic() {

        PubSubBroker broker = new InMemoryPubSubBroker();

        List<String> firstSubscriber = new ArrayList<>();

        List<String> secondSubscriber = new ArrayList<>();

        broker.subscribe("orders", firstSubscriber::add);

        broker.subscribe("orders", secondSubscriber::add);

        broker.publish("orders", "order-created");

        assertEquals(List.of("order-created"), firstSubscriber);

        assertEquals(List.of("order-created"), secondSubscriber);

    }

    @Test
    void shouldNotDeliverMessageToDifferentTopicSubscribers() {

        PubSubBroker broker = new InMemoryPubSubBroker();

        List<String> orders = new ArrayList<>();

        List<String> payments = new ArrayList<>();

        broker.subscribe("orders", orders::add);

        broker.subscribe("payments", payments::add);

        broker.publish("orders", "order-created");

        assertEquals(List.of("order-created"), orders);

        assertEquals(List.of(), payments);

    }

    @Test
    void shouldNotDeliverMessagesAfterUnsubscribe() {

        PubSubBroker broker = new InMemoryPubSubBroker();

        List<String> received = new ArrayList<>();

        Subscription subscription = broker.subscribe("orders", received::add);

        broker.publish("orders", "first-message");

        subscription.close();

        broker.publish("orders", "second-message");

        assertEquals(List.of("first-message"), received);

    }

    @Test
    void shouldPreserveMessageOrderForSubscriber() {

        PubSubBroker broker = new InMemoryPubSubBroker();

        List<String> received = new ArrayList<>();

        broker.subscribe("orders", received::add);

        broker.publish("orders", "first");

        broker.publish("orders", "second");

        broker.publish("orders", "third");

        assertEquals(List.of("first", "second", "third"), received);

    }
}
