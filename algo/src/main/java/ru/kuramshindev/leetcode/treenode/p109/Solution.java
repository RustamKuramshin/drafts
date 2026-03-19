package ru.kuramshindev.leetcode.treenode.p109;

import ru.kuramshindev.leetcode.LeetCodeUtils.ListNode;
import ru.kuramshindev.leetcode.LeetCodeUtils.TreeNode;

import java.util.ArrayList;
import java.util.List;

// https://leetcode.com/problems/convert-sorted-list-to-binary-search-tree/
public class Solution {

    private TreeNode buildBalancedBst(List<Integer> values, int left, int right) {
        if (left > right) {
            return null;
        }

        int middle = (left + right + 1) / 2;
        TreeNode root = new TreeNode(values.get(middle));
        root.left = buildBalancedBst(values, left, middle - 1);
        root.right = buildBalancedBst(values, middle + 1, right);
        return root;
    }

    public TreeNode sortedListToBST(ListNode head) {
        if (head == null) {
            return null;
        }

        List<Integer> values = new ArrayList<>();
        ListNode current = head;

        while (current != null) {
            values.add(current.val);
            current = current.next;
        }

        return buildBalancedBst(values, 0, values.size() - 1);
    }
}
