package com.rustam.dev.leetcode.p460;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// https://leetcode.com/problems/lfu-cache/
public class LFUCache {

    private CacheStore cacheStore;
    private Queue queue;

    private int capacity;
    private int lfuCacheSize;

    private static class Queue {

        private Node front;
        private Node rear;

        public Node getNewNode(int key, int value) {
            return new Node(key, value);
        }

        public boolean isOnlyNodeInQueue(Node node) {
            boolean res = false;
            if (!isEmpty()) {
                if (node.isSingleton()) {
                    if (front == rear) {
                        if (front == node) {
                            res = true;
                        }
                    }
                }
            }

            return res;
        }

        public boolean isEmpty() {
            return (front == null) && (rear == null);
        }

        public void leftShiftToNextUseCounterValue(Node node) {

            if (isEmpty()) {
                if (node.isSingleton()) {
                    front = node;
                    rear = node;
                    return;
                }
            }

            if (isOnlyNodeInQueue(node)) {
                ++node.useCounter;
                return;
            }



        }

        public Node getNodeForInvalidate() {

            Node rearNode = rear;
            rear = rear.prev;
            rear.next = null;

            return rearNode;
        }
    }

    private static class CacheStore {

        private Node[] cache;

        public CacheStore(int capacity) {
            cache = new Node[100000];
        }

        public void put(int key, Node node) {
            cache[key] = node;
        }

        public void remove(int key) {
            cache[key] = null;
        }

        public Node get(int key) {
            return cache[key];
        }

        public int size() {
            int size = 0;
            for (Node node : cache) {
                if (node != null) {
                    ++size;
                }
            }
            return size;
        }
    }

    private static class Node {

        int key;
        int val;
        int useCounter = 1;

        Node prev;
        Node next;

        public Node(int key, int val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return key == node.key && val == node.val && useCounter == node.useCounter;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, val, useCounter);
        }

        public boolean isSingleton() {
            return (prev == null) && (next == null);
        }

        public boolean isRear() {
            return (!isSingleton()) && (prev != null) && (next == null);
        }

        public boolean isFront() {
            return (!isSingleton()) && (prev == null) && (next != null);
        }

        public boolean isMiddle() {
            return (!isSingleton()) && (prev != null) && (next != null);
        }

        public void swap(Node nodeForSwap) {
            if (this == nodeForSwap) return;

            Node thisNodePrev = this.prev;
            Node thisNodeNext = this.next;

            Node nodeForSwapPrev = nodeForSwap.prev;
            Node nodeForSwapNext = nodeForSwap.next;

            this.prev = nodeForSwapPrev;
            this.next = nodeForSwapNext;

            nodeForSwap.prev = thisNodePrev;
            nodeForSwap.next = thisNodeNext;
        }
    }

    private Node invalidate() {

        Node nodeForDelete = queue.getNodeForInvalidate();
        cacheStore.remove(nodeForDelete.key);
        --lfuCacheSize;

        return nodeForDelete;
    }

    public LFUCache(int capacity) {
        cacheStore = new CacheStore(capacity);
        queue = new Queue();
        this.capacity = capacity;
        lfuCacheSize = 0;
    }

    public int get(int key) {

        Node existingNode = cacheStore.get(key);

        if (existingNode != null) {
            queue.leftShiftToNextUseCounterValue(existingNode);
        }

        return existingNode == null ? -1 : existingNode.val;
    }

    private void putNodeInLFUCache(Node newNode, Node existingNode) {

        if (existingNode == null) {
            cacheStore.put(newNode.key, newNode);
            queue.leftShiftToNextUseCounterValue(newNode);
            ++lfuCacheSize;
        } else {
            existingNode.val = newNode.val;
            queue.leftShiftToNextUseCounterValue(existingNode);
        }
    }

    public void put(int key, int value) {

        Node existingNode = cacheStore.get(key);

        if (existingNode == null) {
            if (lfuCacheSize == capacity) {
                invalidate();
            }
        }

        Node newNode = queue.getNewNode(key, value);
        putNodeInLFUCache(newNode, existingNode);
    }

    public static void main(String[] args) {
        // Ниже идут методы не участвующие в решение на leetcode
    }

    private void throwCacheInconsistentException(String cause, String action) {
        throw new IllegalStateException(String.format("Cache state is inconsistent after action=%s! Cause=%s", action, cause));
    }

    private void validateCacheSateAfterAction(String action) {
        // Расчеты кол-ва элементов
        int cacheNodesInCacheStore = cacheStore.size();

        int cacheNodesInLinkedList = 0;
        Node node = queue.front;
        while (node != null) {
            ++cacheNodesInLinkedList;
            if (cacheNodesInLinkedList > cacheNodesInCacheStore) {
                throwCacheInconsistentException("Бесконечный связный список", action);
            }
            node = node.next;
        }

        // Проверки списка
        if (queue.front == null && queue.rear != null) {
            throwCacheInconsistentException("Сломан связный список (rear)", action);
        }

        if (queue.rear == null && queue.front != null) {
            throwCacheInconsistentException("Сломан связный список (front)", action);
        }

        // Проверка кол-ва nodes
        if (cacheNodesInCacheStore != cacheNodesInLinkedList) {
            throwCacheInconsistentException("Кол-во nodes не совпадает", action);
        }

    }

    public List<Integer> toList() {

        List<Integer> integerList = new ArrayList<>();

        Node node = queue.front;
        while (node != null) {
            integerList.add(node.val);
            node = node.next;
        }

        return integerList;
    }

    public int cnt(int key) {
        return cacheStore.get(key).useCounter;
    }

    public String nodeToString(Node node) {
        return String.format("[key=%s,val=%s, uc=%s]", node.key, node.val, node.useCounter);
    }

    public List<String> toListV2() {

        List<String> integerList = new ArrayList<>();

        Node node = queue.front;
        while (node != null) {
            integerList.add(nodeToString(node));
            node = node.next;
        }

        return integerList;
    }
}
