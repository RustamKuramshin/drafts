package com.rustam.dev;

import java.util.*;

// https://leetcode.com/problems/lfu-cache/
public class LFUCache {

    private CacheManager cm;

    private boolean debug = false;

    // ноды очереди
    private class Node {

        public int key;
        public int val;
        // счетчик использования
        public int useCounter = 1;

        public Queue owner;

        public Node prev;
        public Node next;

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
                    ", uc=" + useCounter +
                    '}';
        }

        // очистить связи ноды
        public void clearLinks() {
            owner = null;
            prev = null;
            next = null;
        }

        // нода есть голова очереди
        public boolean isFront() {
            if (owner.isOnlyThisNodeInQueue(this)) {
                return true;
            }
            if (!isSingleton()) {
                return (prev == null) && (next != null);
            }
            return false;
        }

        // нода из середины очереди
        public boolean isMiddle() {
            if (owner.isOnlyThisNodeInQueue(this)) {
                return true;
            }
            if (!isSingleton()) {
                return (prev != null) && (next != null);
            }
            return false;
        }

        // нода есть хвост очереди
        public boolean isRear() {
            if (owner.isOnlyThisNodeInQueue(this)) {
                return true;
            }
            if (!isSingleton()) {
                return (prev != null) && (next == null);
            }
            return false;
        }

        // нода без соседей - одиночка (необязательно новая нода)
        public boolean isSingleton() {
            return (prev == null) && (next == null);
        }
    }

    // очередь
    private class Queue {

        // голова
        private Node front;
        // хвост
        private Node rear;

        public Queue(Node node) {
            if (!node.isSingleton()) {
                throw new IllegalStateException("Нельзя создать очередь с нодой, которая не одиночка");
            }
            front = node;
            rear = node;
        }

        private List<Node> toList() {

            List<Node> nodeList = new ArrayList<>();

            Node node = front;
            while (node != null) {
                nodeList.add(node);
                node = node.next;
            }

            return nodeList;
        }

        @Override
        public String toString() {
            return toList().toString();
        }

        // очистить очередь
        public void clear() {
            front = null;
            rear = null;
        }

        // удалить ноду из очереди
        public void removeNode(Node node) {
            if (node.owner != this) {
                throw new RuntimeException("Нельзя удалить ноду из очереди. Нода не принадлежит очереди");
            }

            if (isEmpty()) return;

            if (isOnlyThisNodeInQueue(node)) {
                clear();
                node.clearLinks();
                return;
            }

            if (node.isFront()) {
                Node nextNode = front.next;
                nextNode.prev = null;
                front = nextNode;
                node.clearLinks();
                return;
            }

            if (node.isMiddle()) {
                Node prevNode = node.prev;
                Node nextNode = node.next;

                prevNode.next = nextNode;
                nextNode.prev = prevNode;

                node.clearLinks();
                return;
            }

            if (node.isRear()) {
                Node prevNode = rear.prev;
                prevNode.next = null;
                rear = prevNode;
                node.clearLinks();
                return;
            }
        }

        // вставить ноду перед головой
        public void insertNodeBeforeFront(Node nodeForInsert) {
            Node frontNode = front;
            frontNode.prev = nodeForInsert;
            nodeForInsert.next = frontNode;
            front = nodeForInsert;
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
    }

    // хранилище кэша
    private class CacheStore {

        private Node[] cache;

        public CacheStore(int capacity) {
            cache = new Node[100000];
        }

        @Override
        public String toString() {
            Map<Integer, Node> nodeMap = new HashMap<>();

            for (int i = 0; i < cache.length - 1; i++) {
                Node node = cache[i];
                if (node != null) {
                    nodeMap.put(i, node);
                }
            }

            return "CacheStore{" +
                    "cache=" + nodeMap +
                    '}';
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
    }

    // статистика кэша
    private class CacheStats {

        private TreeMap<Integer, Queue> stats;

        @Override
        public String toString() {
            return "CacheStats{" +
                    "stats=" + stats +
                    '}';
        }

        public CacheStats() {
            this.stats = new TreeMap<>();
        }

        private void removeNodeFromQueue(Queue queue, Node node) {
            queue.removeNode(node);

            if (queue.isEmpty()) {
                stats.remove(node.useCounter);
            }
        }

        // создать очередь и поместить ноду или поместить ноду в существующую очередь
        private void createOrInsertInQueueForNode(Node node) {
            Queue currentQueue = stats.get(node.useCounter);
            if (currentQueue == null) {
                Queue queue = new Queue(node);
                node.owner = queue;
                stats.put(node.useCounter, queue);
            } else {
                node.owner = currentQueue;
                currentQueue.insertNodeBeforeFront(node);
            }
        }

        // создать статистику для ноды
        public void createStatsForNode(Node node) {
            createOrInsertInQueueForNode(node);
        }

        // обновить статистику для ноды
        public void updateStatsForNode(Node node) {
            Queue currentQueue = stats.get(node.useCounter);

            if (currentQueue == null) {
                throw new RuntimeException("Почему-то для существующей ноды нет очереди в статистике");
            }

            removeNodeFromQueue(currentQueue, node);

            ++node.useCounter;

            createOrInsertInQueueForNode(node);
        }

        // получить ноду для ивалидации (удаления из кэша)
        public Node getNodeForInvalidate() {
            Map.Entry<Integer, Queue> min = stats.firstEntry();
            Queue minQueue = min.getValue();
            return minQueue.rear;
        }

        public void removeNode(Node node) {
            Queue currentQueue = stats.get(node.useCounter);

            if (currentQueue == null) {
                throw new RuntimeException("Нельзя удалить ноду из статистики, если для нее нет очереди");
            }

            removeNodeFromQueue(currentQueue, node);
        }
    }

    // менеджер кэша
    private class CacheManager {

        private CacheStore cacheStore;
        private CacheStats cacheStats;

        private int capacity;
        private int lfuCacheSize;

        public CacheManager(int capacity, CacheStore cacheStore, CacheStats cacheStats) {
            this.capacity = capacity;
            this.cacheStore = cacheStore;
            this.cacheStats = cacheStats;
        }

        public Integer get(int key) {
            Node possibleExistingNode = cacheStore.get(key);
            if (possibleExistingNode != null) {
                cacheStats.updateStatsForNode(possibleExistingNode);
            }
            return possibleExistingNode != null ? possibleExistingNode.val : null;
        }

        public void insertKeyValue(int key, int value) {
            Node newNode = getNewNode(key, value);
            cacheStore.put(key, newNode);
            cacheStats.createStatsForNode(newNode);
            ++lfuCacheSize;
        }

        public void updateKeyValue(int key, int value) {
            Node existingNode = cacheStore.get(key);
            existingNode.val = value;
            cacheStats.updateStatsForNode(existingNode);
        }

        // Получение новой ноды
        private Node getNewNode(int key, int value) {
            return new Node(key, value);
        }

        // провести инвалидацию
        private Node invalidate() {
            Node nodeForDelete = cacheStats.getNodeForInvalidate();

            cacheStats.removeNode(nodeForDelete);
            cacheStore.remove(nodeForDelete.key);

            --lfuCacheSize;
            return nodeForDelete;
        }

        // кэш полон
        public boolean isCacheFull() {
            return lfuCacheSize == capacity;
        }

        // ключ существует в кэше
        public boolean isKeyExist(int key) {
            return cacheStore.get(key) != null;
        }
    }

    // конструктор LFU-кэша
    public LFUCache(int capacity) {
        cm = new CacheManager(
                capacity,
                new CacheStore(capacity),
                new CacheStats()
        );
    }

    // получить значение по ключу из LFU-кэша
    public int get(int key) {
        Integer possibleExistingValue = cm.get(key);
        int res = possibleExistingValue == null ? -1 : possibleExistingValue;

        logDebug("get(key=%s) => %s\n", key, res);

        return res;
    }

    // помещением/обновлением значения по ключу в LFU-кэш
    public void put(int key, int value) {
        if (cm.isKeyExist(key)) {
            cm.updateKeyValue(key, value);
        } else {
            if (cm.isCacheFull()) {
                cm.invalidate();
            }
            cm.insertKeyValue(key, value);
        }
        logDebug("put(key=%s, value=%s)\n", key, value);
    }

    public static void main(String[] args) {
        // Ниже идут методы не участвующие в решение на leetcode
    }

    private void logDebug(String msg, Object... o) {
        if (debug) {
            System.out.println("<=======BEGIN=========");
            System.out.println("ACTION:");
            System.out.printf(msg, o);
            System.out.println("STATE:");
            System.out.printf("cache store: %s\n", cm.cacheStore);
            System.out.printf("cache stats: %s\n", cm.cacheStats);
            System.out.println("========END========>");
        }
    }
}