package ru.kuramshindev.leetcode.treenode.p100;

import static ru.kuramshindev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/same-tree/description/
class Solution {

    public boolean isSameTree(TreeNode p, TreeNode q) {
        if ((p == null) && (q == null)) {
            return true;
        }

        if ((p == null) && (q != null)) {
            return false;
        }

        if ((q == null) && (p != null)) {
            return false;
        }

        if (p.val != q.val) {
            return false;
        }

        return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
    }
}
