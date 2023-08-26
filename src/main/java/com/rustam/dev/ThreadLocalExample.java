package com.rustam.dev;

public class ThreadLocalExample {

    // Создание объекта ThreadLocal для хранения Integer
    private static ThreadLocal<Integer> threadLocal = ThreadLocal.withInitial(() -> 0);

    public static int get() {
        return threadLocal.get();
    }

    public static void increment() {
        threadLocal.set(threadLocal.get() + 1);
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            increment();
            System.out.println("Thread 1: " + get());
        });

        Thread t2 = new Thread(() -> {
            increment();
            increment();
            System.out.println("Thread 2: " + get());
        });

        t1.start();
        t2.start();
    }
}