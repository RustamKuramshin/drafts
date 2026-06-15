package ru.kuramshindev;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CallbackSchedulerImpl implements CallbackScheduler {

    private static final Long FIRE_TIME = 100L;

    private final Map<Instant, List<Runnable>> store;

    private final Executor executor;

    public CallbackSchedulerImpl() {

        store = new TreeMap<>();

        executor = Executors.newCachedThreadPool();

        Thread t = new Thread(() -> {

            while (true) {

                for (Instant inst : store.keySet()) {
                    if (inst.isBefore(Instant.now())) {
                        store.get(inst).forEach(executor::execute);
                        store.remove(inst);
                    }
                }

                try {
                    Thread.sleep(FIRE_TIME);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        t.start();
    }

    @Override
    public void schedule(Runnable callback, Instant when) {
        if (!store.containsKey(when)) {
            store.put(when, new ArrayList<>(List.of(callback)));
        } else {
            var callbacks = store.get(when);
            callbacks.add(callback);
        }
    }

    @Override
    public void close() throws Exception {

    }
}