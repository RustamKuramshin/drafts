package com.rustam.dev.leetcode.node.p460;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

// https://leetcode.com/problems/lfu-cache/
public class LFUCache {

    private CacheManager cm;

    private boolean debug = false;

    // ноды очереди
    private class QueueNode {

        public int key;
        public int val;
        // счетчик использования
        public int useCounter = 1;

        public Queue owner;

        public QueueNode prev;
        public QueueNode next;

        public QueueNode(int key, int val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueueNode queueNode = (QueueNode) o;
            return key == queueNode.key && val == queueNode.val && useCounter == queueNode.useCounter;
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
        private QueueNode front;
        // хвост
        private QueueNode rear;

        public Queue(QueueNode queueNode) {
            if (!queueNode.isSingleton()) {
                throw new IllegalStateException("Нельзя создать очередь с нодой, которая не одиночка");
            }
            front = queueNode;
            rear = queueNode;
        }

        private List<QueueNode> toList() {

            List<QueueNode> queueNodeList = new ArrayList<>();

            QueueNode queueNode = front;
            while (queueNode != null) {
                queueNodeList.add(queueNode);
                queueNode = queueNode.next;
            }

            return queueNodeList;
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
        public void removeNode(QueueNode queueNode) {
            if (queueNode.owner != this) {
                throw new RuntimeException("Нельзя удалить ноду из очереди. Нода не принадлежит очереди");
            }

            if (isEmpty()) return;

            if (isOnlyThisNodeInQueue(queueNode)) {
                clear();
                queueNode.clearLinks();
                return;
            }

            if (queueNode.isFront()) {
                QueueNode nextQueueNode = front.next;
                nextQueueNode.prev = null;
                front = nextQueueNode;
                queueNode.clearLinks();
                return;
            }

            if (queueNode.isMiddle()) {
                QueueNode prevQueueNode = queueNode.prev;
                QueueNode nextQueueNode = queueNode.next;

                prevQueueNode.next = nextQueueNode;
                nextQueueNode.prev = prevQueueNode;

                queueNode.clearLinks();
                return;
            }

            if (queueNode.isRear()) {
                QueueNode prevQueueNode = rear.prev;
                prevQueueNode.next = null;
                rear = prevQueueNode;
                queueNode.clearLinks();
                return;
            }
        }

        // вставить ноду перед головой
        public void insertNodeBeforeFront(QueueNode queueNodeForInsert) {
            QueueNode frontQueueNode = front;
            frontQueueNode.prev = queueNodeForInsert;
            queueNodeForInsert.next = frontQueueNode;
            front = queueNodeForInsert;
        }

        // очередь состоит из одной этой ноды
        public boolean isOnlyThisNodeInQueue(QueueNode queueNode) {
            boolean res = false;
            if (!isEmpty()) {
                if (queueNode.isSingleton()) {
                    if (front == rear) {
                        if (front == queueNode) {
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

        private QueueNode[] cache;

        public CacheStore(int capacity) {
            cache = new QueueNode[100000];
        }

        @Override
        public String toString() {
            Map<Integer, QueueNode> nodeMap = new HashMap<>();

            for (int i = 0; i < cache.length - 1; i++) {
                QueueNode queueNode = cache[i];
                if (queueNode != null) {
                    nodeMap.put(i, queueNode);
                }
            }

            return "CacheStore{" +
                    "cache=" + nodeMap +
                    '}';
        }

        // поместить/обновить ноду в кэше
        public void put(int key, QueueNode queueNode) {
            cache[key] = queueNode;
        }

        // удалить ноду из кэша
        public void remove(int key) {
            cache[key] = null;
        }

        // получить ноду из кэша по ключу
        public QueueNode get(int key) {
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

        private void removeNodeFromQueue(Queue queue, QueueNode queueNode) {
            queue.removeNode(queueNode);

            if (queue.isEmpty()) {
                stats.remove(queueNode.useCounter);
            }
        }

        // создать очередь и поместить ноду или поместить ноду в существующую очередь
        private void createOrInsertInQueueForNode(QueueNode queueNode) {
            Queue currentQueue = stats.get(queueNode.useCounter);
            if (currentQueue == null) {
                Queue queue = new Queue(queueNode);
                queueNode.owner = queue;
                stats.put(queueNode.useCounter, queue);
            } else {
                queueNode.owner = currentQueue;
                currentQueue.insertNodeBeforeFront(queueNode);
            }
        }

        // создать статистику для ноды
        public void createStatsForNode(QueueNode queueNode) {
            createOrInsertInQueueForNode(queueNode);
        }

        // обновить статистику для ноды
        public void updateStatsForNode(QueueNode queueNode) {
            Queue currentQueue = stats.get(queueNode.useCounter);

            if (currentQueue == null) {
                throw new RuntimeException("Почему-то для существующей ноды нет очереди в статистике");
            }

            removeNodeFromQueue(currentQueue, queueNode);

            ++queueNode.useCounter;

            createOrInsertInQueueForNode(queueNode);
        }

        // получить ноду для ивалидации (удаления из кэша)
        public QueueNode getNodeForInvalidate() {
            Map.Entry<Integer, Queue> min = stats.firstEntry();
            Queue minQueue = min.getValue();
            return minQueue.rear;
        }

        public void removeNode(QueueNode queueNode) {
            Queue currentQueue = stats.get(queueNode.useCounter);

            if (currentQueue == null) {
                throw new RuntimeException("Нельзя удалить ноду из статистики, если для нее нет очереди");
            }

            removeNodeFromQueue(currentQueue, queueNode);
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
            QueueNode possibleExistingQueueNode = cacheStore.get(key);
            if (possibleExistingQueueNode != null) {
                cacheStats.updateStatsForNode(possibleExistingQueueNode);
            }
            return possibleExistingQueueNode != null ? possibleExistingQueueNode.val : null;
        }

        public void insertKeyValue(int key, int value) {
            QueueNode newQueueNode = getNewNode(key, value);
            cacheStore.put(key, newQueueNode);
            cacheStats.createStatsForNode(newQueueNode);
            ++lfuCacheSize;
        }

        public void updateKeyValue(int key, int value) {
            QueueNode existingQueueNode = cacheStore.get(key);
            existingQueueNode.val = value;
            cacheStats.updateStatsForNode(existingQueueNode);
        }

        // Получение новой ноды
        private QueueNode getNewNode(int key, int value) {
            return new QueueNode(key, value);
        }

        // провести инвалидацию
        private QueueNode invalidate() {
            QueueNode queueNodeForDelete = cacheStats.getNodeForInvalidate();

            cacheStats.removeNode(queueNodeForDelete);
            cacheStore.remove(queueNodeForDelete.key);

            --lfuCacheSize;
            return queueNodeForDelete;
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