package com.rustam.dev.concurrency;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Random;

public class FactorialPerThread {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        System.out.printf("Start time: %s\n", LocalDateTime.now());
        var count = 0L;
        while (System.currentTimeMillis() - startTime <= 60000) {
            Thread myThread = new Thread(() -> {
                var initNum = new Random().nextInt(10000 - 1000) + 1000;
                var i = 1;
                BigInteger res = BigInteger.valueOf(1);
                while (i < initNum) {
                    ++i;
                    res = res.multiply(BigInteger.valueOf(i));
                }
            });
            myThread.start();
            ++count;
        }
        System.out.printf("End time: %s\n", LocalDateTime.now());
        System.out.printf("Total count: %s\n", count);
    }
}