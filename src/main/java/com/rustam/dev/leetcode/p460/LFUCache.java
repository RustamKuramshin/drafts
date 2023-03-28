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

    // обёртка очереди
    private class Queue {

        // голова
        private Node front;
        // хвост
        private Node rear;

        // Получение новой ноды
        public Node getNewNode(int key, int value) {
            return new Node(key, value);
        }

        public boolean isOneNodeQueue() {
            boolean res = false;
            if (!isEmpty()) {
                if (front == rear) {
                    if (front.next == null) {
                        res = true;
                    }
                }
            }

            return res;
        }

        // очередь состоит из одной этой ноды
        public boolean isOnlyThisNodeInQueue(Node node) {
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

        // очередь пуста
        public boolean isEmpty() {
            return (front == null) && (rear == null);
        }

        public void insertNodeBeforeFront(Node nodeForInsert) {
            Node frontNode = front;
            frontNode.prev = nodeForInsert;
            nodeForInsert.next = frontNode;
            front = nodeForInsert;
        }

        public void insertNodeLeft(Node currentNode, Node nodeForInsert) {
            if (currentNode.isFront()) {
                insertNodeBeforeFront(nodeForInsert);
                return;
            }

            Node leftNode = currentNode.prev;
            currentNode.prev = nodeForInsert;
            nodeForInsert.next = currentNode;
            nodeForInsert.prev = leftNode;
            leftNode.next = nodeForInsert;
        }

        public void insertNodeAfterRear(Node nodeForInsert) {
            Node rearNode = rear;
            rearNode.next = nodeForInsert;
            nodeForInsert.prev = rearNode;
            rear = nodeForInsert;
        }

        public void insertNodeRight(Node currentNode, Node nodeForInsert) {
            if (currentNode.isRear()) {
                insertNodeAfterRear(nodeForInsert);
                return;
            }

            Node rightNode = currentNode.next;
            currentNode.next = nodeForInsert;
            nodeForInsert.prev = currentNode;
            nodeForInsert.next = rightNode;
            rightNode.prev = nodeForInsert;
        }

        // алгоритм продвижения нод в очереди
        public void insertOrShiftNode(Node node) {

            // если очередь пуста и нода одиночка, поместим её в очередь
            if (isEmpty()) {
                if (node.isSingleton()) {
                    front = node;
                    rear = node;
                    return;
                }
            }

            // если очередь состоит из одной этой ноды, то просто накручиваем счетчик
            if (isOnlyThisNodeInQueue(node)) {
                ++node.useCounter;
                return;
            }

            // если нода голова очереди, то просто накручиваем счётчик
            if (node.isFront()) {
                ++node.useCounter;
                return;
            }

            // вставка новой ноды в непустую очередь состоящую из одной ноды
            if (isOneNodeQueue() && node.isNewNode()) {
                if (front.useCounter == node.useCounter) {
                    insertNodeBeforeFront(node);
                    return;
                }

                if (rear.useCounter > node.useCounter) {
                    insertNodeAfterRear(node);
                    return;
                }
            }

            // вставка новой ноды в непустую очередь
            if (!isEmpty() && node.isNewNode()) {
                if (rear.useCounter == node.useCounter) {
                    Node n = rear.prev;
                    if (n.isFront()) {
                        insertNodeBeforeFront(node);
                        return;
                    }
                    while (n.useCounter == node.useCounter) {
                        if (n.isFront()) {
                            insertNodeBeforeFront(node);
                            return;
                        }
                        n = n.prev;
                    }
                    insertNodeRight(n, node);
                    return;
                }

                if (rear.useCounter > node.useCounter) {
                    insertNodeAfterRear(node);
                    return;
                }
            }

            throw new RuntimeException(String.format("Не удалось применить смещение к ноде: %s", node));
        }

        // получить ноду для ивалидации (удаления из кэша)
        public Node getNodeForInvalidate() {

            Node rearNode = rear;
            rear = rear.prev;
            rear.next = null;

            return rearNode;
        }
    }

    // обёртка хранилища кэша
    private class CacheStore {

        private Node[] cache;

        public CacheStore(int capacity) {
            cache = new Node[100000];
        }

        // поместить/обновить ноду в кэше
        public void put(int key, Node node) {
            cache[key] = node;
        }

        // удалить ноду из кэша
        public void remove(int key) {
            cache[key] = null;
        }

        // получить ноду из кэша по ключу
        public Node get(int key) {
            return cache[key];
        }

        // посчитать размер кэша, если нужно (не для отправки)
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

    // ноды кэша
    private class Node {

        int key;
        int val;
        // счетчик использования
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

        @Override
        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", val=" + val +
                    ", useCounter=" + useCounter +
                    '}';
        }

        // новая нода (новая нода всегда одиночка)
        public boolean isNewNode() {
            return (isSingleton()) && (useCounter == 1);
        }

        // нода без соседей, одиночка (необязательно новая нода)
        public boolean isSingleton() {
            return (prev == null) && (next == null);
        }

        // нода есть хвост очереди
        public boolean isRear() {
            if (queue.isOnlyThisNodeInQueue(this)) {
                return true;
            }

            if (!isSingleton()) {
                return (prev != null) && (next == null);
            }

            return false;
        }

        // нода есть голова очереди
        public boolean isFront() {
            if (queue.isOnlyThisNodeInQueue(this)) {
                return true;
            }

            if (!isSingleton()) {
                return (prev == null) && (next != null);
            }

            return false;
        }

        // нода из середины очереди
        public boolean isMiddle() {
            if (queue.isOnlyThisNodeInQueue(this)) {
                return true;
            }

            if (!isSingleton()) {
                return (prev != null) && (next != null);
            }

            return false;
        }

        // обменять местами две ноды
        public void swapTo(Node nodeForSwap) {
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

    // конструктор LFU-кэша
    public LFUCache(int capacity) {
        cacheStore = new CacheStore(capacity);
        queue = new Queue();
        this.capacity = capacity;
        lfuCacheSize = 0;
    }

    // получить по ключу из LFU-кэша
    public int get(int key) {

        Node existingNode = cacheStore.get(key);

        if (existingNode != null) {
            queue.insertOrShiftNode(existingNode);
        }

        return existingNode == null ? -1 : existingNode.val;
    }

    // положить/обновить в LFU-кэше по ключу
    private void putNodeInLFUCache(Node newNode, Node existingNode) {

        if (existingNode == null) {
            cacheStore.put(newNode.key, newNode);
            queue.insertOrShiftNode(newNode);
            ++lfuCacheSize;
        } else {
            existingNode.val = newNode.val;
            queue.insertOrShiftNode(existingNode);
        }
    }

    // провести инвалидацию LFU-кэша
    private Node invalidate() {

        Node nodeForDelete = queue.getNodeForInvalidate();
        cacheStore.remove(nodeForDelete.key);
        --lfuCacheSize;

        return nodeForDelete;
    }

    // проверка на переполнение перед помещением/обновлением
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
