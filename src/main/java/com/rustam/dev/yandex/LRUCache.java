package com.rustam.dev.yandex;

/**
 * Реализуйте LRU-кэш без использования классов из java.util.
 * Суть в том, что нужно самому реализовать требуемые структуры данных
 * и операции над ними, без использования поставляемых jdk инструментов.
 */
class LRUCache<K, V> {

    /**
     * LRU (Least Recently Used) кэш - это структура данных,
     * которая удаляет наименее недавно используемый элемент, когда она достигает своей емкости.
     * Для реализации LRU-кэша обычно используется комбинация двусвязного списка и хэш-карты.
     * Реализация:
     * Двусвязный список используется для сохранения ключей, по которым происходил доступ.
     * Самый недавно использованный ключ перемещается в начало, а наименее недавно использованный - в конец.
     * Если кэш достигает своей максимальной емкости, элемент из конца списка (наименее недавно использованный) удаляется.
     *
     * Хэш-карта хранит ключи и указатели на узлы в двусвязном списке.
     * Это позволяет быстро проверять наличие ключа в кэше и быстро получать к нему доступ.
     */

    private final int capacity;
    private Node<K, V> head, tail;
    private CustomHashMap<K, Node<K, V>> map;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new CustomHashMap<>();
    }

    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) return null;

        moveToHead(node);
        return node.value;
    }

    public void put(K key, V value) {
        Node<K, V> node = map.get(key);

        if (node == null) {
            node = new Node<>(key, value);

            if (map.size() >= capacity) {
                map.remove(tail.key);
                removeNode(tail);
            }

            addNode(node);
            map.put(key, node);
        } else {
            node.value = value;
            moveToHead(node);
        }
    }

    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addNode(node);
    }

    private void addNode(Node<K, V> node) {
        node.prev = null;
        node.next = head;

        if (head != null) {
            head.prev = node;
        }

        head = node;

        if (tail == null) {
            tail = node;
        }
    }

    private void removeNode(Node<K, V> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }

        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
    }

    static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev, next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    // Простая реализация хэш-карты без учета коллизий.
    static class CustomHashMap<K, V> {
        Node<K, V>[] table;
        int size = 0;

        @SuppressWarnings("unchecked")
        public CustomHashMap() {
            table = new Node[10000];
        }

        public V get(K key) {
            int index = getIndex(key);
            Node<K, V> node = table[index];
            return node == null ? null : node.value;
        }

        public void put(K key, V value) {
            int index = getIndex(key);
            table[index] = new Node<>(key, value);
            size++;
        }

        public void remove(K key) {
            int index = getIndex(key);
            table[index] = null;
            size--;
        }

        public int size() {
            return size;
        }

        private int getIndex(K key) {
            return key.hashCode() % table.length;
        }
    }
}

