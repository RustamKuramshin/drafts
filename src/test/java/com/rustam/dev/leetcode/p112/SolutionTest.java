package com.rustam.dev.leetcode.p112;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование метода проверки существования пути в двоичном дереве с заданной суммой")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {

        Solution s = new Solution();

        TreeNode root = new TreeNode(5,
                new TreeNode(4, new TreeNode(11, new TreeNode(7), new TreeNode(2)), null),
                new TreeNode(8, new TreeNode(13), new TreeNode(4, null, new TreeNode(1))));

        assertTrue(s.hasPathSum(root, 22));
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {

        Solution s = new Solution();

        TreeNode root = new TreeNode(1, new TreeNode(2), new TreeNode(3));

        assertFalse(s.hasPathSum(root, 5));
    }

    @Test
    @DisplayName("Базовый пример Example 3")
    public void test_example_3() {

        Solution s = new Solution();

        assertFalse(s.hasPathSum(null, 0));
    }
}


