package com.rustam.dev.leetcode.p700;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тестирования метода поиска в двоичном дереве поиска")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {
        // arrange
        TreeNode root = TreeNode.ofArrayString("[4,2,7,1,3]");
        int val = 2;
        TreeNode expected = TreeNode.ofArrayString("[2,1,3]");

        // act
        Solution s = new Solution();
        TreeNode actual = s.searchBST(root, val);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {
        // arrange
        TreeNode root = TreeNode.ofArrayString("[4,2,7,1,3]");
        int val = 5;
        TreeNode expected = null;

        // act
        Solution s = new Solution();
        TreeNode actual = s.searchBST(root, val);

        // assert
        assertEquals(expected, actual);
    }
}
