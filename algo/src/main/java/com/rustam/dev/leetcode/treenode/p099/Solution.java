package com.rustam.dev.leetcode.treenode.p099;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/recover-binary-search-tree/
public class Solution {

    private void swapVal(List<TreeNode> listTreeNode) {
        if (listTreeNode.size() == 2) {
            TreeNode node1 = listTreeNode.get(0);
            TreeNode node2 = listTreeNode.get(1);

            int node1Val = node1.val;
            int node2Val = node2.val;

            node1.val = node2Val;
            node2.val = node1Val;
        } else {
            throw new IllegalArgumentException("The list must contain exactly two elements");
        }
    }

    private void inOrderTraversal(TreeNode root, List<TreeNode> listTreeNode) {
        if (root == null) return;
        inOrderTraversal(root.left, listTreeNode);
        listTreeNode.add(root);
        inOrderTraversal(root.right, listTreeNode);
    }

    public void recoverTree(TreeNode root) {
        List<TreeNode> ltn = new ArrayList<>();
        inOrderTraversal(root, ltn);
        int ltnSize = ltn.size();

        List<TreeNode> sortedLtn = ltn.stream().sorted(Comparator.comparingInt(n -> n.val)).toList();

        List<TreeNode> badNodes = new ArrayList<>();

        for (int i = 0; i < ltnSize; i++) {
            if (ltn.get(i).val != sortedLtn.get(i).val) {
                badNodes.add(ltn.get(i));
            }
        }

        swapVal(badNodes);
    }

    public static void main(String[] args) {

    }

    private void printListTreeNode(List<TreeNode> listTreeNode) {
        List<Integer> l = listTreeNode.stream().map(tn -> tn.val).toList();
        System.out.println(l);
    }
}
