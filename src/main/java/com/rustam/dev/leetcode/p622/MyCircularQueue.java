package com.rustam.dev.leetcode.p622;

// Разработайте свою реализацию циклической очереди.
// Круговая очередь представляет собой линейную структуру данных, в которой операции выполняются по принципу FIFO (First In First Out),
// а последняя позиция соединяется с первой позицией, образуя круг. Его также называют «кольцевым буфером».

//Одним из преимуществ циклической очереди является то, что мы можем использовать пространство перед очередью.
// В обычной очереди, когда очередь заполняется, мы не можем вставить следующий элемент, даже если перед очередью есть пробел.
// Но используя циклическую очередь, мы можем использовать пространство для хранения новых значений.
public class MyCircularQueue {

    private int enQueuedCount = 0;

    private static class Node {

        Node prev;
        Node next;

        int val;

        public Node() {
        }

        public Node(int val) {
            this.val = val;
        }
    }

    // Инициализирует объект размером очереди k
    public MyCircularQueue(int k) {

    }

    // Вставляет элемент в циклическую очередь. Возвращает true, если операция выполнена успешно
    public boolean enQueue(int value) {
        return false;
    }

    // Удаляет элемент из циклической очереди. Возвращает true, если операция выполнена успешно
    public boolean deQueue() {
        return false;
    }

    // Получает первый элемент из очереди. Если очередь пуста, вернуть -1
    public int Front() {
        return 0;
    }

    // Получает последний элемент из очереди. Если очередь пуста, вернуть -1
    public int Rear() {
        return 0;
    }

    // Проверяет, пуста ли круговая очередь
    public boolean isEmpty() {
        return false;
    }

    // Проверяет, заполнена ли круговая очередь
    public boolean isFull() {
        return false;
    }

    public static void main(String[] args) {

        MyCircularQueue myCircularQueue = new MyCircularQueue(3);

        myCircularQueue.enQueue(1); // return True
        myCircularQueue.enQueue(2); // return True
        myCircularQueue.enQueue(3); // return True
        myCircularQueue.enQueue(4); // return False

        myCircularQueue.Rear();     // return 3
        myCircularQueue.isFull();   // return True

        myCircularQueue.deQueue();  // return True
        myCircularQueue.enQueue(4); // return True

        myCircularQueue.Rear();     // return 4
    }
}
