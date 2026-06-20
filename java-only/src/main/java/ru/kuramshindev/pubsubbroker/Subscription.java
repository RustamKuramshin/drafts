package ru.kuramshindev.pubsubbroker;

public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
