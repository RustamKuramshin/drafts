package com.rustam.dev.leetcode.p146;

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

    private static class Node {

        int key;
        int val;

        Node prev;
        Node next;

        Node(int val) {
            this.val = val;
        }

        Node(int key, int val) {
            this.key = key;
            this.val = val;
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
        Node nextNode = this.head.next;
        this.head = nextNode;
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

        if (this.table[key] == null) {
            ++this.size;
            if (this.size > this.capacity) {
                Node h = popHead();
                logDebug("pop %s\n", h.val);
                --this.size;
                this.table[h.key] = null;
            }
        }

        this.printCache();
        this.table[key] = newNode;
    }

    public static void main(String[] args) {

//        LRUCache lRUCache1 = new LRUCache(2);
//        lRUCache1.put(1, 1); // cache is {1=1}
//        lRUCache1.put(2, 2); // cache is {1=1, 2=2}
//        lRUCache1.get(1);    // return 1
//        lRUCache1.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
//        lRUCache1.get(2);    // returns -1 (not found)
//        lRUCache1.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
//        lRUCache1.get(1);    // return -1 (not found)
//        lRUCache1.get(3);    // return 3
//        lRUCache1.get(4);    // return 4

        LRUCache lRUCache2 = new LRUCache(1);
        lRUCache2.put(2, 1);
        lRUCache2.get(2);


    }

    private static void printFromNode(Node fromNode) {

        if (fromNode == null) {
            System.out.println("[]");
            return;
        }

        StringBuilder res = new StringBuilder();
        res.append("[");

        Node node = fromNode;
        do {
            res.append(node.val);
            node = node.next;
            if (node != null) res.append(", ");
        } while (node != null);

        res.append("]");

        System.out.println(res);
    }

    private void printCache() {
        System.out.print("cache: ");
        printFromNode(this.head);
    }
}
