package com.rustam.dev.leetcode.p112;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/path-sum/
public class Solution {

    private boolean res = false;
    private int ts;

    private void findPathsRootToLeaf(TreeNode node, int sum) {
        if (node == null) return;
        if (res) return;

        sum = sum + node.val;

        if (node.left == null && node.right == null) {
            if (sum == ts) {
                res = true;
            }
        } else {
            findPathsRootToLeaf(node.left, sum);
            findPathsRootToLeaf(node.right, sum);
        }
    }

    public boolean hasPathSum(TreeNode root, int targetSum) {
        ts = targetSum;
        findPathsRootToLeaf(root, 0);
        return res;
    }
}
