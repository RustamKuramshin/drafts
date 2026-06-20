package ru.kuramshindev;

import java.util.function.Consumer;

public interface PubSubBroker {
    Subscription subscribe(String topic, Consumer<String> handler);
    void publish(String topic, String message);
}
