package com.rustam.dev.leetcode.treenode.p297;

import static com.rustam.dev.leetcode.LeetCodeUtils.*;

import java.util.List;
import java.util.ArrayList;

// https://leetcode.com/problems/binary-tree-paths/description/
class Solution {

    private void findPaths(TreeNode node, String path, List<String> treePaths) {

        if (node.left == null && node.right == null) {
            treePaths.add(path + node.val);
        }

        if (node.left != null) {
            findPaths(node.left, path + node.val + "->", treePaths);
        }

        if (node.right != null) {
            findPaths(node.right, path + node.val + "->", treePaths);
        }

    }

    public List<String> binaryTreePaths(TreeNode root) {

        List<String> treePaths = new ArrayList<>();

        if (root != null) {
            findPaths(root, "", treePaths);
        }

        return treePaths;
    }
}
