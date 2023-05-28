package com.rustam.dev.leetcode.node.p622.array;

// Разработайте свою реализацию циклической очереди.
// Круговая очередь представляет собой линейную структуру данных, в которой операции выполняются по принципу FIFO (First In First Out),
// а последняя позиция соединяется с первой позицией, образуя круг. Его также называют «кольцевым буфером».

import com.rustam.dev.leetcode.node.p622.IMyCircularQueue;

// Одним из преимуществ циклической очереди является то, что мы можем использовать пространство перед очередью.
// В обычной очереди, когда очередь заполняется, мы не можем вставить следующий элемент, даже если перед очередью есть пробел.
// Но используя циклическую очередь, мы можем использовать пространство для хранения новых значений.
public class MyCircularQueue implements IMyCircularQueue {

    private int[] queue;

    private int front = 0;
    private int rear = -1;

    private int enQueuedCount = 0;

    // Инициализирует объект размером очереди k
    public MyCircularQueue(int k) {
        this.queue = new int[k];
        this.queue[0] = -1;
    }

    // Вставляет элемент в циклическую очередь. Возвращает true, если операция выполнена успешно
    public boolean enQueue(int value) {

        if (isFull()) return false;

        if (rear == queue.length - 1) rear = -1;

        queue[++rear] = value;
        ++enQueuedCount;

        return true;
    }

    // Удаляет элемент из циклической очереди. Возвращает true, если операция выполнена успешно
    public boolean deQueue() {

        if (isEmpty()) return false;

        queue[front++] = -1;
        --enQueuedCount;

        if (isEmpty()) {
            front = 0;
            rear = -1;
        }

        return true;
    }

    // Получает первый элемент из очереди. Если очередь пуста, вернуть -1
    public int Front() {
        if (isEmpty()) return -1;
        return queue[front];
    }

    // Получает последний элемент из очереди. Если очередь пуста, вернуть -1
    public int Rear() {
        if (isEmpty()) return -1;
        return queue[rear];
    }

    // Проверяет, пуста ли круговая очередь
    public boolean isEmpty() {
        return enQueuedCount == 0;
    }

    // Проверяет, заполнена ли круговая очередь
    public boolean isFull() {
        return enQueuedCount == queue.length;
    }
}
