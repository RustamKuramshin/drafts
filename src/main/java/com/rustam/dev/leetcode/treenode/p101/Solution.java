package com.rustam.dev.leetcode.treenode.p101;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static com.rustam.dev.leetcode.LeetCodeUtils.TreeNode;

// https://leetcode.com/problems/symmetric-tree/
public class Solution {

    private boolean allNull(List<Integer> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) != -1000) {
                return false;
            }
        }
        return true;
    }

    private void trimList(List<Integer> list) {
        for (int i = list.size(); i-- > 0; ) {
            if (list.get(i) == -1000) {
                list.remove(i);
            }
        }
    }

    private boolean isSymmetricList(List<Integer> list) {

        trimList(list);

        if (list.size() == 1) return true;


        if (list.size() == 0) return true;

        for (int i = 0; i < list.size(); i++) {
            int val1 = list.get(i);
            int val2 = list.get(list.size() - 1 - i);

            if (val1 != val2) {
                return false;
            }
        }

        return true;
    }

    public boolean isSymmetric(TreeNode root) {

        if (root.left == null && root.right == null) return true;

        Queue<TreeNode> queue = new LinkedList<>();
        List<Integer> list = new ArrayList<>();

        queue.add(root);

        int level = 0;

        while (!queue.isEmpty()) {

            TreeNode treeNode = queue.remove();

            list.add(treeNode.val);

            if (list.size() == (1 << level)) {
                if (!isSymmetricList(list)) {
                    System.out.println("1 " + list);
                    return false;
                }

                list.clear();
                ++level;
            }

            if (treeNode.left == null && treeNode.right == null) {

                if (treeNode.val != -1000) {
                    queue.add(new TreeNode(-1000));
                    queue.add(new TreeNode(-1000));
                }

                continue;
            }

            if (treeNode.left != null) {
                queue.add(treeNode.left);
            } else {
                queue.add(new TreeNode(-1000));
            }

            if (treeNode.right != null) {
                queue.add(treeNode.right);
            } else {
                queue.add(new TreeNode(-1000));
            }
        }

        if (list.size() != 0 && allNull(list)) {
            System.out.println("2 " + list);
            return false;
        }

        if (list.size() != 0 && isSymmetricList(list)) {
            System.out.println("3 " + list);
            return true;
        }

        System.out.println("4 " + list);
        return list.size() == 0;
    }
}