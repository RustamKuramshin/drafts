package com.rustam.dev.leetcode.treenode.p653;

import java.util.HashMap;
import java.util.Map;

import static com.rustam.dev.leetcode.LeetCodeUtils.*;

// https://leetcode.com/problems/two-sum-iv-input-is-a-bst/
public class Solution {

    private boolean traversal(TreeNode node, Map<Integer, Integer> pairs, int k) {

        if (node == null) {
            return false;
        }

        int comp = k - node.val;

        if (pairs.containsKey(comp)) {
            return true;
        }

        pairs.put(node.val, 0);

        return traversal(node.left, pairs, k) || traversal(node.right, pairs, k);
    }

    public boolean findTarget(TreeNode root, int k) {
        Map<Integer, Integer> pairs = new HashMap<>();
        return traversal(root, pairs, k);
    }
}
