package com.rustam.dev.leetcode.p622;

// Разработайте свою реализацию циклической очереди.
// Круговая очередь представляет собой линейную структуру данных, в которой операции выполняются по принципу FIFO (First In First Out),
// а последняя позиция соединяется с первой позицией, образуя круг. Его также называют «кольцевым буфером».

// Одним из преимуществ циклической очереди является то, что мы можем использовать пространство перед очередью.
// В обычной очереди, когда очередь заполняется, мы не можем вставить следующий элемент, даже если перед очередью есть пробел.
// Но используя циклическую очередь, мы можем использовать пространство для хранения новых значений.
public class MyCircularQueue {

    private int enQueuedCount = 0;
    private int maxQueueSize = 0;
    private boolean isClosed = false;

    private Node front;
    private Node rear;

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
        this.maxQueueSize = k;
    }

    // Вставляет элемент в циклическую очередь. Возвращает true, если операция выполнена успешно
    public boolean enQueue(int value) {

        if (isFull()) return false;

        if (isEmpty() && !isClosed) {
            Node newNode = new Node(value);
            front = newNode;
            rear = newNode;
        } else if (!isClosed) {
            Node newNode = new Node(value);
            rear.next = newNode;
            rear = rear.next;
        } else {
            rear = rear.next;
            rear.val = value;
        }

        ++enQueuedCount;

        if (rear.next == null && isFull()) {
            rear.next = front;
            isClosed = true;
        }

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

    public static void main(String[] args) {
        test1();
        test2();
    }

    private static void test1() {

        System.out.println("============TEST 1============");

        MyCircularQueue myCircularQueue = new MyCircularQueue(3);

        System.out.println(myCircularQueue.enQueue(1)); // return true
        System.out.println(myCircularQueue.enQueue(2)); // return true
        System.out.println(myCircularQueue.enQueue(3)); // return true
        System.out.println(myCircularQueue.enQueue(4)); // return false

        System.out.println(myCircularQueue.Rear()); // return 3
        System.out.println(myCircularQueue.isFull()); // return true

        System.out.println(myCircularQueue.deQueue()); // return true
        System.out.println(myCircularQueue.enQueue(4)); // return true

        System.out.println(myCircularQueue.Rear()); // return 4
    }

    private static void test2() {

        System.out.println("============TEST 2============");

        MyCircularQueue rb = new MyCircularQueue(1);

        System.out.println(rb.enQueue(1));
        System.out.println(rb.enQueue(2));
        System.out.println(rb.deQueue());
        System.out.println(rb.Rear());
    }
}
