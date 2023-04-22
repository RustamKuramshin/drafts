package com.rustam.dev.leetcode.p020;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестирование метода валидатора скобочек")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {

        Solution s = new Solution();

        assertTrue(s.isValid("()"));
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {

        Solution s = new Solution();

        assertTrue(s.isValid("()[]{}"));
    }

    @Test
    @DisplayName("Базовый пример Example 3")
    public void test_example_3() {

        Solution s = new Solution();

        assertFalse(s.isValid("(]"));
    }

    @Test
    @DisplayName("my case 1")
    public void test_my_case_1() {

        Solution s = new Solution();

        assertTrue(s.isValid("([()])"));
    }

    @Test
    @DisplayName("my case 2")
    public void test_my_case_2() {

        Solution s = new Solution();

        assertFalse(s.isValid("([()]}"));
    }

    @Test
    @DisplayName("my case 3")
    public void test_my_case_3() {

        Solution s = new Solution();

        assertTrue(s.isValid("([()]){}"));
    }

    @Test
    @DisplayName("test case 91")
    public void test_case_91() {

        Solution s = new Solution();

        assertFalse(s.isValid("(){}}{"));
    }
}
