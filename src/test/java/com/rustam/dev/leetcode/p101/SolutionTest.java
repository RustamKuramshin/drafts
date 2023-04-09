package com.rustam.dev.leetcode.p101;

import com.rustam.dev.leetcode.TreeNode;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестирование метода проверки симметричности двоичного дерева")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {

        Solution s = new Solution();

        TreeNode tn_left_1 = new TreeNode(3);
        TreeNode tn_right_2 = new TreeNode(4);
        TreeNode tn_left_3 = new TreeNode(2, tn_left_1, tn_right_2);

        TreeNode tn_left_4 = new TreeNode(4);
        TreeNode tn_right_5 = new TreeNode(3);
        TreeNode tn_right_6 = new TreeNode(2, tn_left_4, tn_right_5);

        TreeNode root = new TreeNode(1, tn_left_3, tn_right_6);

        assertTrue(s.isSymmetric(root));
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {

        Solution s = new Solution();

        TreeNode tn_right_1 = new TreeNode(3);
        TreeNode tn_left_1 = new TreeNode(2);
        tn_left_1.right = tn_right_1;

        TreeNode tn_right_2 = new TreeNode(3);
        TreeNode tn_right_3 = new TreeNode(2);
        tn_right_3.right = tn_right_2;

        TreeNode root = new TreeNode(1, tn_left_1, tn_right_3);

        assertFalse(s.isSymmetric(root));
    }

    @Test
    @DisplayName("test_1")
    public void test_1() {

        Solution s = new Solution();

        TreeNode root = new TreeNode(1);

        assertTrue(s.isSymmetric(root));
    }

    @Test
    @DisplayName("test case 196")
    public void test_case_196() {

        Solution s = new Solution();

        TreeNode tn_right_1 = new TreeNode(2);
        TreeNode tn_left_1 = new TreeNode(2);
        tn_left_1.left = tn_right_1;

        TreeNode tn_right_2 = new TreeNode(2);
        TreeNode tn_right_3 = new TreeNode(2);
        tn_right_3.left = tn_right_2;

        TreeNode root = new TreeNode(1, tn_left_1, tn_right_3);

        assertFalse(s.isSymmetric(root));
    }
}
