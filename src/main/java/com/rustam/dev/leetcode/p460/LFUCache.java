package com.rustam.dev.leetcode.p460;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LFUCache {

    private Node[] cache;
    private Node front;
    private Node rear;

    private int capacity;
    private int currentSize;

    private static class Node {

        int key = -1;
        int val = -1;
        int useCounter = 1;

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

    private void removeNodeFromQueue(Node node) {

        Node prevNode = node.prev;
        Node nextNode = node.next;

        // удаление из середины
        if (prevNode != null && nextNode != null) {
            prevNode.next = nextNode;
            nextNode.prev = prevNode;
        }

        // удаление с головы
        if (prevNode == null) {
            front = nextNode;
            nextNode.prev = null;
        }

        // удаление с хвоста
        if (nextNode == null) {
            rear = prevNode;
            prevNode.next = null;
        }

        node.prev = null;
        node.next = null;
    }

    private void insertFront(Node node) {
        if (node == null) return;

        if (front == null) {
            front = node;
            rear = node;
        } else {
            node.next = front;
            front.prev = node;
            front = node;
        }
    }

    private void moveToFront(Node node) {
        if (front == null) {
            insertFront(node);
            return;
        }
        if (node.key != front.key) {
            removeNodeFromQueue(node);
            insertFront(node);
        }
    }

    private Node popRear() {
        if (rear == null) return null;

        Node rearNode = rear;
        rear = rear.prev;
        rear.next = null;
        return rearNode;
    }

    private void putNodeInCache(Node node) {

        Node currentNode = cache[node.key];

        if (currentNode == null) {
            cache[node.key] = node;
            insertFront(node);
        } else {
            currentNode.val = node.val;
            ++currentNode.useCounter;
            moveToFront(currentNode);
        }

    }

    private List<Node> getNodesWithMinUseCounter() {
        int min = front.useCounter;
        List<Node> minNodes = new ArrayList<>();

        Node node = front;
        while (node != null) {
            if (node.useCounter == min) {
                minNodes.add(node);
            }
            if (node.useCounter < min) {
                min = node.useCounter;
                minNodes.clear();
                minNodes.add(node);
            }
            node = node.next;
        }

        return minNodes;
    }

    private Node invalidate() {
        List<Node> minNodes = getNodesWithMinUseCounter();
        Node deletedNode = null;

        if (minNodes.size() == 1) {
            deletedNode = minNodes.get(0);
        } else if (minNodes.size() > 1) {
            Optional<Node> opn = minNodes.stream().filter(n -> Objects.isNull(n.next)).findFirst();
            if (opn.isPresent()) {
                deletedNode = opn.get();
            }
        }

        removeNodeFromQueue(deletedNode);
        cache[deletedNode.key] = null;

        return deletedNode;
    }

    public LFUCache(int capacity) {
        cache = new Node[100000];
        this.capacity = capacity;
        currentSize = 0;
    }

    public int get(int key) {
        Node node = cache[key];

        if (node != null) {
            if (!(node.prev == null && node.next == null)) {
                moveToFront(node);

            }
            ++node.useCounter;
        }

        int res = node == null ? -1 : node.val;

        return res;
    }

    public void put(int key, int value) {

        Node newNode = new Node(key, value);

        if (currentSize == capacity) {
            invalidate();
            --currentSize;
        }

        putNodeInCache(newNode);
        ++currentSize;
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

    public int cnt(int key) {
        return cache[key].useCounter;
    }
}
