package com.rustam.dev.leetcode.p527;

import com.rustam.dev.leetcode.p572.Solution;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестирование метода проверки вхождения поддерева в двоичное дерево")
public class SolutionTest {

    @Test
    @DisplayName("test same tree")
    public void test_same_tree() {
        // arrange
        TreeNode root = TreeNode.ofArrayString("[3,4,5,1,2]");
        TreeNode subRoot = TreeNode.ofArrayString("[3,4,5,1,2]");
        boolean expected = true;

        // act
        Solution s = new Solution();
        boolean actual = s.isSubtree(root, subRoot);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("test root null")
    public void test_root_null() {
        // arrange
        TreeNode root = null;
        TreeNode subRoot = TreeNode.ofArrayString("[3,4,5,1,2]");
        boolean expected = false;

        // act
        Solution s = new Solution();
        boolean actual = s.isSubtree(root, subRoot);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {
        // arrange
        TreeNode root = TreeNode.ofArrayString("[3,4,5,1,2]");
        TreeNode subRoot = TreeNode.ofArrayString("[4,1,2]");
        boolean expected = true;

        // act
        Solution s = new Solution();
        boolean actual = s.isSubtree(root, subRoot);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {
        // arrange
        TreeNode root = TreeNode.ofArrayString("[3,4,5,1,2,null,null,null,null,0]");
        TreeNode subRoot = TreeNode.ofArrayString("[4,1,2]");
        boolean expected = false;

        // act
        Solution s = new Solution();
        boolean actual = s.isSubtree(root, subRoot);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("test equals")
    public void test_equals() {

        Solution s = new Solution();

        TreeNode r1 = TreeNode.ofArrayString("[3,4,5,1,2]");
        TreeNode r2 = TreeNode.ofArrayString("[3,4,5,1,2]");

        assertTrue(s.equals(r1, r2));

        TreeNode r3 = TreeNode.ofArrayString("[3,4,5,1,1]");
        TreeNode r4 = TreeNode.ofArrayString("[3,4,5,1,2]");

        assertFalse(s.equals(r3, r4));

        TreeNode r5 = TreeNode.ofArrayString("[3,4,5,1,2,null,null,null,null,0]");
        TreeNode r6 = TreeNode.ofArrayString("[4,1,2]");

        assertFalse(s.equals(r5, r6));

        TreeNode r7 = TreeNode.ofArrayString("[3]");
        TreeNode r8 = TreeNode.ofArrayString("[3]");

        assertTrue(s.equals(r7, r8));

        TreeNode r9 = TreeNode.ofArrayString("[4,1,2,null,null,0,null]");
        TreeNode r10 = TreeNode.ofArrayString("[4,1,2]");

        assertFalse(s.equals(r9, r10));

        TreeNode r11 = TreeNode.ofArrayString("[4,1,2]");
        TreeNode r12 = TreeNode.ofArrayString("[4,1,2]");

        assertTrue(s.equals(r11, r12));
    }
}
