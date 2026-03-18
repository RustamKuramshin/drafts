package com.rustam.dev.leetcode.treenode.p617;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/merge-two-binary-trees/
public class Solution {

    private TreeNode doubleTraverse(TreeNode root1, TreeNode root2) {
        if (root1 != null && root2 != null) {
            TreeNode treeNode = new TreeNode();
            treeNode.val = root1.val + root2.val;

            treeNode.left = doubleTraverse(root1.left, root2.left);
            treeNode.right = doubleTraverse(root1.right, root2.right);

            return treeNode;
        } else if (root1 != null) {
            return root1;
        } else if (root2 != null) {
            return root2;
        } else {
            return null;
        }
    }

    public TreeNode mergeTrees(TreeNode root1, TreeNode root2) {
        return doubleTraverse(root1, root2);
    }
}
