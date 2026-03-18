package com.rustam.dev.concurrency;

import java.time.LocalDateTime;

public class SynchronizedStatements {

    private int counter1 = 0;
    private int counter2 = 0;

    public synchronized void inc1() throws InterruptedException {
        Thread.sleep(1000);
        counter1++;
    }

    public synchronized void inc2() throws InterruptedException {
        Thread.sleep(1000);
        counter2++;
    }

    public static void main(String[] args) {
        SynchronizedStatements ss = new SynchronizedStatements();

        System.out.println(LocalDateTime.now());

        (new Thread(() -> {
            try {
                ss.inc1();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(LocalDateTime.now());
        })).start();

        (new Thread(() -> {
            try {
                ss.inc2();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(LocalDateTime.now());
        })).start();
    }
}
