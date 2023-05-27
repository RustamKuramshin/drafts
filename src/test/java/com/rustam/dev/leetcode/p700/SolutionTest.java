package com.rustam.dev.leetcode.p700;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;
import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode.PredefinedNodePosition;
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

        root.printBinaryTree();

        // act
        Solution s = new Solution();
        TreeNode actual = s.searchBST(root, val);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("test generate random binary tree")
    public void test_generate_random_binary_tree() {

        // генерация случайного двоичного дерева TreeNode
        TreeNode tn = TreeNode.randomBinaryTreeBuilder()
                .maxNodesCount(5_000)
                .minNodeVal(-10_000)
                .maxNodeVal(10_000)
                .build();

        System.out.printf("Tree size: %s\n", tn.size());
        System.out.println();

        // "Красивая" печать двоичного дерева
        tn.printBinaryTree();
    }
}