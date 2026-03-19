package ru.kuramshindev.leetcode.p146;

import ru.kuramshindev.leetcode.LeetCodeUtils;
import ru.kuramshindev.leetcode.node.p146.LRUCache;
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
