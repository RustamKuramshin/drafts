package ru.kuramshindev;

public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
