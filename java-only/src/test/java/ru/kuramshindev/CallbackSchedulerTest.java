package ru.kuramshindev;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.kuramshindev.callbackscheduler.CallbackScheduler;
import ru.kuramshindev.callbackscheduler.CallbackSchedulerImpl;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CallbackSchedulerTest {

    private CallbackScheduler scheduler;

    private static class CallbackResult {
        boolean isDone = false;
    }

    @BeforeEach
    public void setUp() {
        scheduler = new CallbackSchedulerImpl();
    }

    @AfterEach
    public void tearDown() throws Exception {
        scheduler.close();
    }

    @Test
    public void testSimple() throws InterruptedException {

        final CallbackResult result = new CallbackResult();

        scheduler.schedule(
            () -> {
                synchronized (result) {
                    result.isDone = true;
                    result.notify();
                }
            },
            Instant.now().plusSeconds(2)
        );

        synchronized (result) {
            result.wait();
            assertTrue(result.isDone);
        }
    }
}
