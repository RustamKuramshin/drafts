package com.rustam.dev.leetcode.p146;

import com.rustam.dev.leetcode.LeetCodeUtils;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Тестирование LFU-кэша")
public class LRUCacheTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_base_example() {
        LeetCodeUtils.CacheTestCasePlayer.playTestCase(LRUCache.class, "lru-test-cases/methods.txt", "lru-test-cases/data.txt");
    }
}
