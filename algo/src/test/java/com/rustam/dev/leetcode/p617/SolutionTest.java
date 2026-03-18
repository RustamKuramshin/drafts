package com.rustam.dev.leetcode.p617;

import com.rustam.dev.leetcode.treenode.p617.Solution;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тестирование метода слияния двух двоичных деревьев")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {
        // arrange
        TreeNode root1 = TreeNode.ofArrayString("[1,3,2,5]");
        TreeNode root2 = TreeNode.ofArrayString("[2,1,3,null,4,null,7]");
        TreeNode expected = TreeNode.ofArrayString("[3,4,5,5,4,null,7]");

        // act
        Solution s = new Solution();
        TreeNode actual = s.mergeTrees(root1, root2);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {
        // arrange
        TreeNode root1 = TreeNode.ofArrayString("[1]");
        TreeNode root2 = TreeNode.ofArrayString("[1,2]");
        TreeNode expected = TreeNode.ofArrayString("[2,2]");

        // act
        Solution s = new Solution();
        TreeNode actual = s.mergeTrees(root1, root2);

        // assert
        assertEquals(expected, actual);
    }
}
