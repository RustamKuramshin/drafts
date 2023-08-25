package com.rustam.dev;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockExample {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private int data = 0;

    public int readData() {
        readLock.lock();
        try {
            // читаем данные
            return data;
        } finally {
            readLock.unlock();
        }
    }

    public void writeData(int value) {
        writeLock.lock();
        try {
            // модифицируем данные
            data = value;
        } finally {
            writeLock.unlock();
        }
    }
}