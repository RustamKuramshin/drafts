package com.rustam.dev.leetcode.p641;

public class MyCircularDeque {
    
    private int enQueuedCount = 0;
    private int maxQueueSize = 0;

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

    public MyCircularDeque(int k) {
        this.maxQueueSize = k;
    }

    public boolean insertLast(int value) {

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

    public boolean insertFront(int value) {

        if (isFull()) return false;

        Node newNode = new Node(value);

        if (isEmpty()) {
            front = newNode;
            rear = newNode;
        } else {
            newNode.next = front;
            front = newNode;
        }

        ++enQueuedCount;



    }

    public boolean deleteFront() {

        if (isEmpty()) return false;

        front = front.next;
        --enQueuedCount;

        return true;
    }

    public int getFront() {
        if (isEmpty()) return -1;
        return front.val;
    }

    public int getRear() {
        if (isEmpty()) return -1;
        return rear.val;
    }

    public boolean isEmpty() {
        return enQueuedCount == 0;
    }

    public boolean isFull() {
        return enQueuedCount == maxQueueSize;
    }
}
