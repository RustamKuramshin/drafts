package com.rustam.dev.leetcode.p112;

import com.rustam.dev.leetcode.TreeNode;

import java.util.Arrays;

public class Solution {

    private boolean res = false;
    private int ts;

    private int arraySum(int[] arr) {
        return Arrays.stream(arr).sum();
    }

    private void findPathsRootToLeaf(TreeNode node, int[] paths, int idx) {
        if (node == null) return;
        if (res) return;

        paths[idx] = node.val;
        ++idx;

        if (node.left == null && node.right == null) {
            int[] p = Arrays.copyOfRange(paths, 0, idx);

            if (arraySum(p) == ts) {
                res = true;
            }

            System.out.println(Arrays.toString(p));
        } else {
            findPathsRootToLeaf(node.left, paths, idx);
            findPathsRootToLeaf(node.right, paths, idx);
        }
    }

    public boolean hasPathSum(TreeNode root, int targetSum) {
        ts = targetSum;
        int[] paths = new int[5000];
        int i = 0;
        findPathsRootToLeaf(root, paths, i);
        return res;
    }
}
