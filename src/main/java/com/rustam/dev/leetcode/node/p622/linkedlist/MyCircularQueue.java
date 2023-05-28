package com.rustam.dev.leetcode.node.p622.linkedlist;

import com.rustam.dev.leetcode.node.p622.IMyCircularQueue;

import static com.rustam.dev.leetcode.LeetCodeUtils.Node;

public class MyCircularQueue implements IMyCircularQueue {

    private int enQueuedCount = 0;
    private int maxQueueSize = 0;

    private Node front;
    private Node rear;

    // Инициализирует объект размером очереди k
    public MyCircularQueue(int k) {
        this.maxQueueSize = k;
    }

    // Вставляет элемент в циклическую очередь. Возвращает true, если операция выполнена успешно
    public boolean enQueue(int value) {

        if (isFull()) return false;

        Node newNode = new Node(value);

        if (isEmpty()) {
            front = newNode;
            rear = newNode;
        } else {
            rear.next = newNode;
            rear = rear.next;
        }

        ++enQueuedCount;

        return true;
    }

    // Удаляет элемент из циклической очереди. Возвращает true, если операция выполнена успешно
    public boolean deQueue() {

        if (isEmpty()) return false;

        front = front.next;
        --enQueuedCount;

        return true;
    }

    // Получает первый элемент из очереди. Если очередь пуста, вернуть -1
    public int Front() {
        if (isEmpty()) return -1;
        return front.val;
    }

    // Получает последний элемент из очереди. Если очередь пуста, вернуть -1
    public int Rear() {
        if (isEmpty()) return -1;
        return rear.val;
    }

    // Проверяет, пуста ли круговая очередь
    public boolean isEmpty() {
        return enQueuedCount == 0;
    }

    // Проверяет, заполнена ли круговая очередь
    public boolean isFull() {
        return enQueuedCount == maxQueueSize;
    }
}
