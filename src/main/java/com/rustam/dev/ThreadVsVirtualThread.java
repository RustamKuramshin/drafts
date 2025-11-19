package com.rustam.dev;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/*
 * Для запуска из терминала:
 * java src/main/java/com/rustam/dev/ThreadVsVirtualThread.java 20000 10
 */
public class ThreadVsVirtualThread {
    private static final int DEFAULT_TASKS = 10_000;
    private static final Duration DEFAULT_WORK = Duration.ofMillis(10);

    public static void main(String[] args) throws InterruptedException {
        int taskCount = args.length > 0 ? parseIntOrDefault(args[0], DEFAULT_TASKS) : DEFAULT_TASKS;
        Duration simulatedWork = args.length > 1
                ? Duration.ofMillis(parseLongOrDefault(args[1], DEFAULT_WORK.toMillis()))
                : DEFAULT_WORK;

        System.out.printf("Running %,d blocking tasks (~%d ms each)%n", taskCount, simulatedWork.toMillis());
        System.out.println("Virtual threads park while sleeping; platform threads keep the OS thread busy.\n");

        BenchmarkResult platformResult = runPlatformPool(taskCount, simulatedWork);
        BenchmarkResult virtualResult = runVirtualThreads(taskCount, simulatedWork);

        print(platformResult);
        print(virtualResult);

        double speedup = virtualResult.throughputPerSecond() / platformResult.throughputPerSecond();
        System.out.printf(
                "%nVirtual threads handled the workload about %.1fx faster while creating %,d lightweight threads versus %,d OS threads.%n",
                speedup, virtualResult.threadsCreated(), platformResult.threadsCreated());
    }

    private static BenchmarkResult runPlatformPool(int taskCount, Duration simulatedWork) throws InterruptedException {
        int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors());
        CountingThreadFactory factory = new CountingThreadFactory(Thread.ofPlatform().factory(), "platform");
        ExecutorService executor = Executors.newFixedThreadPool(poolSize, factory);
        try {
            return executeBenchmark("Platform fixed pool (" + poolSize + " threads)", executor, factory, taskCount,
                    simulatedWork);
        } finally {
            executor.shutdownNow();
        }
    }

    private static BenchmarkResult runVirtualThreads(int taskCount, Duration simulatedWork) throws InterruptedException {
        CountingThreadFactory factory = new CountingThreadFactory(Thread.ofVirtual().factory(), "virtual");
        ExecutorService executor = Executors.newThreadPerTaskExecutor(factory);
        try {
            return executeBenchmark("Virtual thread per task", executor, factory, taskCount, simulatedWork);
        } finally {
            executor.shutdownNow();
        }
    }

    private static BenchmarkResult executeBenchmark(String name, ExecutorService executor, CountingThreadFactory factory,
            int taskCount, Duration simulatedWork) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(taskCount);
        long start = System.nanoTime();

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(simulatedWork.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.MINUTES) && latch.await(10, TimeUnit.MINUTES);
        long elapsedMs = Math.max(1, Duration.ofNanos(System.nanoTime() - start).toMillis());

        if (!finished) {
            throw new IllegalStateException("Benchmark did not finish in time for " + name);
        }

        return new BenchmarkResult(name, taskCount, factory.createdThreads(), elapsedMs);
    }

    private static void print(BenchmarkResult result) {
        System.out.printf("%n%s%n- threads created: %,d%n- elapsed: %,d ms%n- throughput: %,.0f tasks/s%n",
                result.name(), result.threadsCreated(), result.elapsedMs(), result.throughputPerSecond());
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLongOrDefault(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private record BenchmarkResult(String name, int tasks, long threadsCreated, long elapsedMs) {
        double throughputPerSecond() {
            return tasks / (elapsedMs / 1000.0);
        }
    }

    private static final class CountingThreadFactory implements ThreadFactory {
        private final ThreadFactory delegate;
        private final AtomicLong created = new AtomicLong();
        private final String prefix;

        CountingThreadFactory(ThreadFactory delegate, String prefix) {
            this.delegate = delegate;
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            long count = created.incrementAndGet();
            Thread thread = delegate.newThread(r);
            thread.setName(prefix + "-" + count);
            return thread;
        }

        long createdThreads() {
            return created.get();
        }
    }
}
