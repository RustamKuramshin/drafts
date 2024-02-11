package com.rustam.dev.leetcode.treenode.p783;

import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.rustam.dev.leetcode.LeetCodeUtils.*;

// https://leetcode.com/problems/minimum-distance-between-bst-nodes/
public class Solution {

    private int traversal(TreeNode root, TreeSet<Integer> tm, int[] minDiff) {

        if (root == null) {
            return minDiff[0];
        }

        // ceilingEntry() - возврат element с наименьшим ключом, большим или равным данному
        Integer element = tm.ceiling(root.val);

        if(element != null) {
            minDiff[0] = Math.min(minDiff[0], element - root.val);
        }

        // lowerEntry() - возврат element с наибольшим ключом, строго меньше чем заданный
        element = tm.lower(root.val);

        if (element != null) {
            minDiff[0] = Math.min(minDiff[0], root.val - element);
        }

        tm.add(root.val);

        traversal(root.left, tm, minDiff);
        traversal(root.right, tm, minDiff);

        return minDiff[0];
    }

    public int minDiffInBST(TreeNode root) {
        return traversal(root, new TreeSet<>(), new int[]{Integer.MAX_VALUE});
    }
}
