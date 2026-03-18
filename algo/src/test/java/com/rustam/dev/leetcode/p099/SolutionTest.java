package com.rustam.dev.leetcode.p099;

import com.rustam.dev.leetcode.treenode.p099.Solution;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тестирование метода восстановление структуры двоичного дерева поиска")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {
        //arrange
        TreeNode root = TreeNode.ofArrayString("[1,3,null,null,2]");
        TreeNode expected = TreeNode.ofArrayString("[3,1,null,null,2]");

        //act
        Solution s = new Solution();
        s.recoverTree(root);

        //assert
        assertEquals(expected, root);
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {
        //arrange
        TreeNode root = TreeNode.ofArrayString("[3,1,4,null,null,2]");
        TreeNode expected = TreeNode.ofArrayString("[2,1,4,null,null,3]");

        //act
        Solution s = new Solution();
        s.recoverTree(root);

        //assert
        assertEquals(expected, root);
    }
}
