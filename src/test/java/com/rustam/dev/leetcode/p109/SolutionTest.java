package com.rustam.dev.leetcode.p109;

import com.rustam.dev.leetcode.LeetCodeUtils.ListNode;
import com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;
import com.rustam.dev.leetcode.treenode.p109.Solution;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Тестирование метода конвертации упорядоченного односвязного списка в сбалансированное по высоте двоичное дерево поиска")
public class SolutionTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_example_1() {
        // arrange
        ListNode head = ListNode.ofArrayString("[-10,-3,0,5,9]");
        TreeNode expected = TreeNode.ofArrayString("[0,-3,9,-10,null,5]");

        // act
        Solution s = new Solution();
        TreeNode actual = s.sortedListToBST(head);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Базовый пример Example 2")
    public void test_example_2() {
        // arrange
        ListNode head = null;
        TreeNode expected = null;

        // act
        Solution s = new Solution();
        TreeNode actual = s.sortedListToBST(head);

        // assert
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("test case 7")
    public void test_case_7() {
        // arrange
        ListNode head = ListNode.ofArrayString("[0,1,2,3,4,5]");
        TreeNode expected = TreeNode.ofArrayString("[3,1,5,0,2,4]");

        // act
        Solution s = new Solution();
        TreeNode actual = s.sortedListToBST(head);

        // assert
        expected.printBinaryTree();
        System.out.println();
        actual.printBinaryTree();
        assertEquals(expected, actual);
    }
}
