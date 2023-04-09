package com.rustam.dev.leetcode.p101;

import com.rustam.dev.leetcode.TreeNode;

import java.util.ArrayList;
import java.util.List;

// https://leetcode.com/problems/symmetric-tree/
public class Solution {

    private List<NodeWrap> list = new ArrayList<>();

    private static class NodeWrap {

        private enum Position {
            ROOT, LEFT, RIGHT
        }

        public int val;
        public Position position;

        public NodeWrap(int val, Position position) {
            this.val = val;
            this.position = position;
        }

        @Override
        public String toString() {
            return String.format("%s-%s", val, position);
        }
    }

    private void traverseInOrder(TreeNode node, NodeWrap.Position position) {
        if (node != null) {
            traverseInOrder(node.left, NodeWrap.Position.LEFT);
            list.add(new NodeWrap(node.val, position));
            traverseInOrder(node.right, NodeWrap.Position.RIGHT);
        }
    }

    public boolean isSymmetric(TreeNode root) {
        traverseInOrder(root, NodeWrap.Position.ROOT);

        int ls = list.size();
        int lastIdx = ls - 1;

        System.out.println(list);

        if (ls == 1) return true;

        for (int i = 0; i < ls; i++) {
            NodeWrap nw1 = list.get(i);
            NodeWrap nw2 = list.get(lastIdx - i);

            if ((nw1.position == NodeWrap.Position.ROOT) && (nw2.position == NodeWrap.Position.ROOT)) {
                continue;
            }

            if ((nw1.val != nw2.val) || (nw1.position == nw2.position)) {
                System.out.println(nw1);
                System.out.println(nw2);
                return false;
            }
        }

        return true;
    }

    public static void main(String[] args) {

    }

    //    private void traversePreOrder(TreeNode node) {
//        if (node != null) {
//            list.add(node.val);
//            traversePreOrder(node.left);
//            traversePreOrder(node.right);
//        }
//    }
//
//    private void traversePostOrder(TreeNode node) {
//        if (node != null) {
//            traversePostOrder(node.left);
//            traversePostOrder(node.right);
//            list.add(node.val);
//        }
//    }
}
