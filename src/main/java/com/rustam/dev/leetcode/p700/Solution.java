package com.rustam.dev.leetcode.p700;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/search-in-a-binary-search-tree/
public class Solution {

    public TreeNode searchBST(TreeNode root, int val) {
        if (root != null && val == root.val) {
            return root;
        } else if (root != null && val > root.val) {
            return searchBST(root.right, val);
        } else if (root != null && val < root.val) {
            return searchBST(root.left, val);
        }
        return null;
    }
}
