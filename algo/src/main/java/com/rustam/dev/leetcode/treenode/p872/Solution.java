package com.rustam.dev.leetcode.treenode.p872;

import java.util.ArrayList;
import java.util.List;

import static com.rustam.dev.leetcode.LeetCodeUtils.*;

// https://leetcode.com/problems/leaf-similar-trees/
public class Solution {

    private List<Integer> traversal(TreeNode root, List<Integer> leafs) {

        if (root == null) {
            return leafs;
        }

        if (root.left == null && root.right == null) {
            leafs.add(root.val);
            return leafs;
        }

        traversal(root.left, leafs);
        traversal(root.right, leafs);

        return leafs;
    }

    public boolean leafSimilar(TreeNode root1, TreeNode root2) {
        var leafs1 = traversal(root1, new ArrayList<>());
        var leafs2 = traversal(root2, new ArrayList<>());

        return leafs1.equals(leafs2);
    }
}
