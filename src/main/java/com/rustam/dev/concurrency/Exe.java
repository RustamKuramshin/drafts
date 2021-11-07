package com.rustam.dev.concurrency;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class Exe {

    public static void main(String[] args) throws InterruptedException {

        var e = Executors.newFixedThreadPool(3);

        IntStream.range(0, 100).forEach(c -> {
            e.submit(() -> {
                System.out.printf("Hello from %s\n", Thread.currentThread().getName());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            });
        });

        e.shutdown();

        System.out.println("End of process");
    }
}
