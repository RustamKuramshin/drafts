package com.rustam.dev.leetcode.p622;

public interface IMyCircularQueue {
    // Вставляет элемент в циклическую очередь. Возвращает true, если операция выполнена успешно
    boolean enQueue(int value);

    // Удаляет элемент из циклической очереди. Возвращает true, если операция выполнена успешно
    boolean deQueue();

    // Получает первый элемент из очереди. Если очередь пуста, вернуть -1
    int Front();

    // Получает последний элемент из очереди. Если очередь пуста, вернуть -1
    int Rear();

    // Проверяет, пуста ли круговая очередь
    boolean isEmpty();

    // Проверяет, заполнена ли круговая очередь
    boolean isFull();
}
