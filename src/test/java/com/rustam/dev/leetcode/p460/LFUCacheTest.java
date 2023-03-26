package com.rustam.dev.leetcode.p460;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестирование LFU-кэша")
public class LFUCacheTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_base_example() {

        // cnt(x) = the use counter for key x
        // cache=[] will show the last used order for tiebreakers (leftmost element is  most recent)

        LFUCache lfu = new LFUCache(2);
        lfu.put(1, 1);
        assertArrayEquals(List.of(1).toArray(), lfu.toList().toArray());// cache=[1,_]
        assertEquals(1, lfu.cnt(1)); // cnt(1)=1
        lfu.put(2, 2);
        assertArrayEquals(List.of(2, 1).toArray(), lfu.toList().toArray());// cache=[2,1]
        assertEquals(1, lfu.cnt(2));// cnt(2)=1
        assertEquals(1, lfu.cnt(1));//  cnt(1)=1
        assertEquals(1, lfu.get(1));       // return 1
        assertArrayEquals(List.of(1, 2).toArray(), lfu.toList().toArray()); // cache=[1,2]
        assertEquals(1, lfu.cnt(2));// cnt(2)=1
        assertEquals(2, lfu.cnt(1));// cnt(1)=2
        lfu.put(3, 3);   // 2 is the LFU key because cnt(2)=1 is the smallest, invalidate 2.
        assertArrayEquals(List.of(3, 1).toArray(), lfu.toList().toArray());// cache=[3,1]
        assertEquals(1, lfu.cnt(3)); // cnt(3)=1
        assertEquals(2, lfu.cnt(1)); // cnt(1)=2
        assertEquals(-1, lfu.get(2));      // return -1 (not found)
        assertEquals(3, lfu.get(3));       // return 3
        assertArrayEquals(List.of(3, 1).toArray(), lfu.toList().toArray());// cache=[3,1]
        assertEquals(2, lfu.cnt(3)); // cnt(3)=2
        assertEquals(2, lfu.cnt(1)); // cnt(1)=2
        lfu.put(4, 4);   // Both 1 and 3 have the same cnt, but 1 is LRU, invalidate 1.
        assertArrayEquals(List.of(4, 3).toArray(), lfu.toList().toArray());// cache=[4,3]
        assertEquals(1, lfu.cnt(4)); // cnt(4)=1
        assertEquals(2, lfu.cnt(3)); // cnt(3)=2
        assertEquals(-1, lfu.get(1));      // return -1 (not found)
        assertEquals(3, lfu.get(3));       // return 3
        assertArrayEquals(List.of(3, 4).toArray(), lfu.toList().toArray());// cache=[3,4]
        assertEquals(1, lfu.cnt(4));// cnt(4)=1
        assertEquals(3, lfu.cnt(3));// cnt(3)=3
        assertEquals(4, lfu.get(4));       // return 4
        assertArrayEquals(List.of(4, 3).toArray(), lfu.toList().toArray());// cache=[4,3]
        assertEquals(2, lfu.cnt(4));// cnt(4)=2
        assertEquals(3, lfu.cnt(3));// cnt(3)=3
    }
}
