package com.rustam.dev.leetcode.treenode.p700;

import java.util.LinkedList;
import java.util.Queue;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/search-in-a-binary-search-tree/
public class Solution {

    private TreeNode traverse(TreeNode root, int val) {

        Queue<TreeNode> queue = new LinkedList<>();

        if (root != null) queue.offer(root);

        while (!queue.isEmpty()) {

            TreeNode node = queue.poll();

            if (node.val == val) {
                System.gc();
                return node;
            }

            if (node.left != null && val < node.val) {
                queue.offer(node.left);
            }

            if (node.right != null && val > node.val) {
                queue.offer(node.right);
            }
        }

        return null;
    }

    public TreeNode searchBST(TreeNode root, int val) {
        return traverse(root, val);
    }
}
