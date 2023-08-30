package com.rustam.dev.concurrency;

import java.util.concurrent.Semaphore;

public class SemaphoreExample {

    private static final int MAX_AVAILABLE = 5;
    private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

    public Object getResource() throws InterruptedException {
        available.acquire();
        return getNextAvailableResource();
    }

    public void putResource(Object x) {
        if (markAsUnused(x)) {
            available.release();
        }
    }

    // Не реализованные методы:
    protected Object getNextAvailableResource() {
        // ... логика для получения ресурса
        return new Object();
    }

    protected boolean markAsUnused(Object x) {
        // ... логика для возврата ресурса
        return true;
    }

    public static void main(String[] args) {
        // Пример использования Semaphore
    }
}