package com.rustam.dev.tinkoff;

import java.util.HashMap;
import java.util.Map;

public class MapWithAllUnique<K, V> {

    private Map<K, V> store;
    private Map<V, Integer> uniqMap;

    public MapWithAllUnique() {
        store = new HashMap<>();
        uniqMap = new HashMap<>();
    }

    public V put(K key, V value) {
        V oldValue = store.get(key);

        if (oldValue == null || oldValue.equals(value)) {
            if (uniqMap.containsKey(value)) {
                uniqMap.put(value, uniqMap.get(value) + 1);
            } else {
                uniqMap.put(value, 1);
            }
        } else {
            uniqMap.put(oldValue, uniqMap.get(oldValue) - 1);

            if (uniqMap.get(oldValue) == 0) {
                uniqMap.remove(oldValue);
            }
        }

        return store.put(key, value);
    }

    public V get(K key) {
        return store.get(key);
    }

    public V remove(K key) {
        V removedValue = store.remove(key);

        if (uniqMap.containsKey(removedValue)) {
            uniqMap.put(removedValue, uniqMap.get(removedValue) - 1);

            if (uniqMap.get(removedValue) == 0) {
                uniqMap.remove(removedValue);
            }
        }

        return removedValue;
    }

    public boolean isAllUnique() {
        return store.keySet().size() == uniqMap.size();
    }

    public static void main(String[] args) {
        MapWithAllUnique<String, String> map = new MapWithAllUnique<>();

        map.put("s1", "test0");
        map.put("s2", "test1");

        System.out.println(map.isAllUnique());

        map.put("s3", "test");
        map.put("s4", "test");

        System.out.println(map.isAllUnique());
    }
}