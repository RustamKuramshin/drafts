package ru.kuramshindev.callbackscheduler;

import java.time.Instant;

public interface CallbackScheduler extends AutoCloseable {
    void schedule(Runnable callback, Instant when);
}
