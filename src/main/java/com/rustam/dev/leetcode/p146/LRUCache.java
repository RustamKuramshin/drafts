package com.rustam.dev.leetcode.p146;

public class LRUCache {

    private Node head;
    private Node last;

    private static class Node {

        int val;
        Node prev;
        Node next;

        Node() {

        }

        Node(int x) {
            this.val = x;
        }
    }

    private Node[] table;

    private int capacity;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.table = new Node[10000];
    }

    public int get(int key) {
        Node res = this.table[key];
        return res == null ? -1 : res.val;
    }

    public void put(int key, int value) {

        Node newNode = new Node(value);

        if (this.head == null) {
            this.head = newNode;
            this.last = this.head;
        } else {
            newNode.prev = this.last;
            this.last.next = newNode;
            this.last = newNode;
        }

        this.table[key] = newNode;
    }

    public static void main(String[] args) {

        LRUCache lRUCache = new LRUCache(2);
        lRUCache.put(1, 1); // cache is {1=1}
        lRUCache.put(2, 2); // cache is {1=1, 2=2}
        System.out.println(lRUCache.get(1));    // return 1
        lRUCache.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
        System.out.println(lRUCache.get(2));    // returns -1 (not found)
        lRUCache.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
        System.out.println(lRUCache.get(1));    // return -1 (not found)
        System.out.println(lRUCache.get(3));    // return 3
        System.out.println(lRUCache.get(4));    // return 4
    }
}
