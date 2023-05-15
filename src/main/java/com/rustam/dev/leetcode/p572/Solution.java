package com.rustam.dev.leetcode.p572;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/subtree-of-another-tree/
public class Solution {

    public boolean equals(TreeNode treeNode1, TreeNode treeNode2) {
        if (treeNode1 == treeNode2) return true;
        return treeNode1 != null && treeNode2 != null && treeNode1.val == treeNode2.val &&
                equals(treeNode1.left, treeNode2.left) &&
                equals(treeNode1.right, treeNode2.right);
    }

    private boolean traversal(TreeNode root, TreeNode subRoot, boolean res) {
        if (root == null) {
            System.out.println("recursion base");
            return res;
        }

        System.out.println("tree node value: " + root.val);

        res = equals(root, subRoot);

        if (res) {
            System.out.println("return - 1");
            return true;
        }

        res = traversal(root.left, subRoot, res);
        if (res) {
            System.out.println("return - 2");
            return true;
        }

        res = traversal(root.right, subRoot, res);
        if (res) {
            System.out.println("return - 3");
            return true;
        };

        System.out.println("return - 4");
        return res;
    }

    public boolean isSubtree(TreeNode root, TreeNode subRoot) {
        return traversal(root, subRoot, false);
    }
}
