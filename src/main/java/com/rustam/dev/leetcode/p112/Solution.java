package com.rustam.dev.leetcode.p112;

import com.rustam.dev.leetcode.TreeNode;

public class Solution {

    private void findPathsRootToLeaf(TreeNode node) {
        if (node == null) return;

        if (node.left == null && node.right == null) {
            return;
        } else {
            findPathsRootToLeaf(node.left);
            findPathsRootToLeaf(node.right);
        }
    }

    public boolean hasPathSum(TreeNode root, int targetSum) {
        findPathsRootToLeaf(root);
        return false;
    }
}
