package com.rustam.dev.leetcode.p098;

import com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

import java.util.ArrayList;
import java.util.List;

public class Solution {

    private void inOrderTraversal(TreeNode root, List<TreeNode> listTreeNode) {
        if (root == null) return;
        inOrderTraversal(root.left, listTreeNode);
        listTreeNode.add(root);
        inOrderTraversal(root.right, listTreeNode);
    }

    public boolean isValidBST(TreeNode root) {
        if (root.left == null && root.right == null) return true;

        boolean res = true;

        List<TreeNode> ltn = new ArrayList<>();
        inOrderTraversal(root, ltn);
        int ltnSize = ltn.size();

        for (int i = 0; i < ltnSize; i++) {
            if (i + 1 <= ltnSize - 1) {
                if (ltn.get(i).val >= ltn.get(i+1).val) {
                    res = false;
                    break;
                }
            } else {
                if (ltn.get(i).val <= ltn.get(i-1).val) {
                    res = false;
                    break;
                }
            }
        }

        return res;
    }
}
