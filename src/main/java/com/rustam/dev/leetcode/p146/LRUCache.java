package com.rustam.dev.leetcode.p146;

import static com.rustam.dev.leetcode.LeetCodeUtils.Node;

public class LRUCache {

    private Node[] table;
    private Node head;
    private Node last;

    private int size;
    private int capacity;

    private boolean debug = true;
    private void logDebug(String msg, Object... o) {
        if (debug) {
            System.out.printf(msg, o);
        }
    }

    private void appendNodeToEnd(Node node) {
        node.prev = this.last;
        this.last.next = node;
        this.last = node;
    }

    private void removeNode(Node node) {

        Node prevNode = node.prev;
        Node nextNode = node.next;

        // удаление из середины
        if (prevNode != null && nextNode != null) {
            prevNode.next = nextNode;
            nextNode.prev = prevNode;
        }

        // удаление с головы
        if (prevNode == null) {
            this.head = nextNode;
            nextNode.prev = null;
        }

        // удаление с хвоста
        if (nextNode == null) {
            this.last = prevNode;
            prevNode.next = null;
        }

        node.prev = null;
        node.next = null;
    }

    private Node popHead() {
        Node node = this.head;
        this.head = this.head.next;
        this.head.prev = null;
        return node;
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.table = new Node[10000];
    }

    public int get(int key) {
        Node node = this.table[key];

        if (node != null) {
            if (!(node.prev == null && node.next == null)) {
                removeNode(node);
                appendNodeToEnd(node);
            }
        }

        int res = node == null ? -1 : node.val;
        logDebug("get(key=%s) => %s\n", key, res);
        this.printCache();
        return res;
    }

    public void put(int key, int value) {
        logDebug("put(key=%s, value=%s)\n", key, value);

        Node newNode = new Node(key, value);

        if (this.head == null) {
            this.head = newNode;
            this.last = this.head;
        } else {
            appendNodeToEnd(newNode);
        }

        Node oldNode = this.table[key];

        if (oldNode == null) {
            ++this.size;
            if (this.size > this.capacity) {
                Node h = popHead();
                logDebug("pop %s\n", h.val);
                --this.size;
                this.table[h.key] = null;
            }
        } else {
            removeNode(oldNode);
        }

        this.printCache();
        this.table[key] = newNode;
    }

    public static void main(String[] args) {

        LRUCache lRUCache1 = new LRUCache(2);
        lRUCache1.put(1, 1); // cache is {1=1}
        lRUCache1.put(2, 2); // cache is {1=1, 2=2}
        lRUCache1.get(1);    // return 1
        lRUCache1.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
        lRUCache1.get(2);    // returns -1 (not found)
        lRUCache1.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
        lRUCache1.get(1);    // return -1 (not found)
        lRUCache1.get(3);    // return 3
        lRUCache1.get(4);    // return 4

        LRUCache lRUCache2 = new LRUCache(1);
        lRUCache2.put(2, 1);
        lRUCache2.get(2);

        LRUCache lruCache3 = new LRUCache(2);

        lruCache3.put(2, 1);
        lruCache3.put(1, 1);
        lruCache3.put(2, 3);
        lruCache3.put(4, 1);

        lruCache3.get(1);
        lruCache3.get(2);
    }

    private void printCache() {
        System.out.print("cache: ");
        this.head.printNode();
    }
}
