package com.rustam.dev.leetcode.p572;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/subtree-of-another-tree/
public class Solution {

    private boolean res = false;

    public boolean equals(TreeNode treeNode1, TreeNode treeNode2) {
        if (treeNode1 == treeNode2) return true;
        return treeNode1 != null && treeNode2 != null && treeNode1.val == treeNode2.val &&
                equals(treeNode1.left, treeNode2.left) &&
                equals(treeNode1.right, treeNode2.right);
    }

    private void traversal(TreeNode root, TreeNode subRoot) {
        if (root == null) return;
        if (res) return;
        res = equals(root, subRoot);
        traversal(root.left, subRoot);
        traversal(root.right, subRoot);
    }

    public boolean isSubtree(TreeNode root, TreeNode subRoot) {
        traversal(root, subRoot);
        return res;
    }
}
