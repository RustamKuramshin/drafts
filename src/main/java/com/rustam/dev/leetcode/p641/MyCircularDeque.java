package com.rustam.dev.leetcode.p641;

import java.util.ArrayList;
import java.util.List;

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
            newNode.prev = rear;
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
            front.prev = newNode;
            front = newNode;
        }

        ++enQueuedCount;

        return true;
    }

    public boolean deleteLast() {

        if (isEmpty()) return false;

        rear = rear.prev;
        if (rear != null) {
            rear.next = null;
        }
        --enQueuedCount;

        return true;
    }

    public boolean deleteFront() {

        if (isEmpty()) return false;

        front = front.next;
        if (front != null) {
            front.prev = null;
        }
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

    public static void main(String[] args) {

    }

    public List<Integer> toList() {

        List<Integer> integerList = new ArrayList<>();

        Node node = front;
        while (node != null) {
            integerList.add(node.val);
            node = node.next;
        }

        return integerList;
    }
}
