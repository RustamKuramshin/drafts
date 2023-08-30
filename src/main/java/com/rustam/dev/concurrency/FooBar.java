package com.rustam.dev.concurrency;

import java.time.Instant;

public class FooBar {
    private int n;
    private boolean fooTurn = true;

    public FooBar(int n) {
        this.n = n;
    }

    public synchronized void foo(Runnable printFoo) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            while (!fooTurn) {
                wait();
            }
            // printFoo.run() outputs "foo". Do not change or remove this line.
            printFoo.run();
            fooTurn = false;
            notifyAll();
            Thread.sleep(1000); // Задержка в 1 секунду
        }
    }

    public synchronized void bar(Runnable printBar) throws InterruptedException {
        for (int i = 0; i < n; i++) {
            while (fooTurn) {
                wait();
            }
            // printBar.run() outputs "bar". Do not change or remove this line.
            printBar.run();
            fooTurn = true;
            notifyAll();
            Thread.sleep(500); // Задержка в полсекунды
        }
    }

    public static void main(String[] args) {
        FooBar fooBar = new FooBar(50);

        Thread t1 = new Thread(() -> {
            try {
                fooBar.foo(() -> System.out.println("foo " + Instant.now()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                fooBar.bar(() -> System.out.println("bar " + Instant.now()));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
    }
}


