package com.rustam.dev.leetcode.p617;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

@DisplayName("Тестирование метода слияния двух двоичных деревьев")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {

        TreeNode root1 = TreeNode.of("[1,3,2,5]");
        TreeNode root2 = TreeNode.of("[2,1,3,null,4,null,7]");

        Solution s = new Solution();
        TreeNode actual = s.mergeTrees(root1, root2);

        // expected: [3,4,5,5,4,null,7]
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {

        TreeNode root1 = TreeNode.of("[1]");
        TreeNode root2 = TreeNode.of("[1,2]");

        Solution s = new Solution();
        TreeNode actual = s.mergeTrees(root1, root2);

        // expected: [2,2]
    }
}
