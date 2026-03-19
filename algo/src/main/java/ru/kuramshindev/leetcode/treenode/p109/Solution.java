package ru.kuramshindev.leetcode.treenode.p109;

import ru.kuramshindev.leetcode.LeetCodeUtils.ListNode;
import ru.kuramshindev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/convert-sorted-list-to-binary-search-tree/
public class Solution {

    private TreeNode traverse(TreeNode root, ListNode head) {

        if (head == null) return null;

        TreeNode tn = new TreeNode(head.val);

        root.left = traverse(tn.left ,head.next);

        root.right = traverse(tn.right ,head.next);

        return root;
    }

    public TreeNode sortedListToBST(ListNode head) {
        if (head == null) return null;

        TreeNode res = new TreeNode();

        return traverse(res, head);
    }
}
