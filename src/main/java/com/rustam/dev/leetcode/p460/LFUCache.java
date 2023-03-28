package com.rustam.dev.leetcode.p460;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// https://leetcode.com/problems/lfu-cache/
public class LFUCache {

    private Node[] cache;
    private Node front;
    private Node rear;

    private int capacity;
    private int currentSize;

    private static class Node {

        int key;
        int val;
        int useCounter = 1;
        int priority = 0;

        Node prev;
        Node next;

        Node(int key, int val) {
            this.key = key;
            this.val = val;
        }
    }

    private Node newNode(int key, int value) {

        return new Node(key, value);
    }

    private void incNodeUseCounter(Node node) {

        node.useCounter = node.useCounter + 1;
    }

    private void removeNodeFromQueue(Node node) {

        if (this.front == node && this.rear == node) {
            front = null;
            rear = null;
            return;
        }

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
            node.priority = 1;
            front = node;
            rear = node;
        } else {
            node.priority = front.priority + 1;
            node.next = front;
            front.prev = node;
            front = node;
        }
    }

    private void moveToFront(Node node) {

        if (node.prev == null && node.next == null) {
            insertFront(node);
            return;
        }

        if (node.key != front.key) {
            removeNodeFromQueue(node);
            insertFront(node);
        }

    }

    private Node getNodeForDelete() {

        return null;
    }

    private Node invalidate() {

        Node nodeForDelete = getNodeForDelete();

        cache[nodeForDelete.key] = null;

        return nodeForDelete;
    }

    public LFUCache(int capacity) {
        cache = new Node[100000];
        this.capacity = capacity;
        currentSize = 0;
    }

    public int get(int key) {
        Node node = cache[key];

        if (node != null) {
            moveToFront(node);
            incNodeUseCounter(node);
        }

        int res = node == null ? -1 : node.val;

        return res;
    }

    private void putNodeInCache(Node node) {

        Node currentNode = cache[node.key];

        if (currentNode == null) {
            cache[node.key] = node;
            moveToFront(node);
        } else {
            currentNode.val = node.val;
            incNodeUseCounter(currentNode);
            moveToFront(currentNode);
        }
    }

    public void put(int key, int value) {

        Node currentNode = cache[key];

        if (currentNode == null) {
            if (currentSize == capacity) {
                invalidate();
                --currentSize;
            }
        }

        Node newNode = newNode(key, value);

        putNodeInCache(newNode);
        if (currentNode == null) {
            ++currentSize;
        }
    }

    public static void main(String[] args) {
        // Ниже идут методы не участвующие в решение на leetcode
    }

    private void throwCacheInconsistentException(String cause, String action) {
        throw new IllegalStateException(String.format("Cache state is inconsistent after action=%s! Cause=%s", action, cause));
    }

    private void validateCacheSateAfterAction(String action) {
        // Расчеты кол-ва элементов
        int cacheNodesInArray = 0;
        for (Node cn : cache) {
            if (cn != null) {
                ++cacheNodesInArray;
            }
        }

        int cacheNodesInLinkedList = 0;
        Node node = front;
        while (node != null) {
            ++cacheNodesInLinkedList;
            if (cacheNodesInLinkedList > cache.length) {
                throwCacheInconsistentException("Бесконечный связный список", action);
            }
            node = node.next;
        }

        // Проверки списка
        if (front == null && rear != null) {
            throwCacheInconsistentException("Сломан связный список (rear)", action);
        }

        if (rear == null && front != null) {
            throwCacheInconsistentException("Сломан связный список (front)", action);
        }

        // Проверка кол-ва nodes
        if (cacheNodesInArray != cacheNodesInLinkedList) {
            throwCacheInconsistentException("Кол-во nodes не совпадает", action);
        }

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

    public String nodeToString(Node node) {
        return String.format("[key=%s,val=%s, uc=%s]", node.key, node.val, node.useCounter);
    }

    public List<String> toListV2() {

        List<String> integerList = new ArrayList<>();

        Node node = front;
        while (node != null) {
            integerList.add(nodeToString(node));
            node = node.next;
        }

        return integerList;
    }
}
