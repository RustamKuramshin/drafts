package com.rustam.dev.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Exe2 {
    public static void main(String[] args) throws InterruptedException, ExecutionException {

        var e = Executors.newSingleThreadExecutor();

        var f = e.submit(() -> {
            Thread.sleep(3000);
            return 42;
        });

        while (!f.isDone()) {
            System.out.println("wait...");
            Thread.sleep(100);
        }

        System.out.printf("Future - %s", f.get());
    }
}
