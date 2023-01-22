package com.rustam.dev.leetcode.p141;

import java.util.HashSet;
import java.util.Set;

public class Solution {

    private static class ListNode {

        int val;
        ListNode next;

        boolean isTail;

        ListNode(int x) {
            val = x;
            next = null;
        }

        ListNode(int x, boolean isTail) {
            this(x);
            this.isTail = isTail;
        }
    }

    private static void printListNode(ListNode listNode) {
        StringBuilder res = new StringBuilder();
        res.append("[");

        ListNode node = listNode;
        do {
            res.append(node.val);
            node = node.next;
            if (node != null) res.append(", ");
        } while (node != null && !node.isTail);

        if (node != null) res.append(node.val);

        res.append("]");

        System.out.println(res);
    }

    public boolean hasCycle(ListNode head) {
//        printListNode(head);

        boolean res = false;

        Set<Integer> hashCodeSet = new HashSet<>();

        ListNode node = head;
        while (node != null) {
            if (!hashCodeSet.contains(node.hashCode())) {
                hashCodeSet.add(node.hashCode());
            } else {
                res = true;
                break;
            }

            node = node.next;
        }

        return res;
    }

    public static void main(String[] args) {
        Solution s = new Solution();

        ListNode ln1 = new ListNode(3);
        ln1.next = new ListNode(2);
        ln1.next.next = new ListNode(0);
        ln1.next.next.next = new ListNode(-4, true);
        ln1.next.next.next.next = ln1.next;

        System.out.println(s.hasCycle(ln1)); // [3, 2, 0, -4], pos = 1 -> true

        ListNode ln2 = new ListNode(1);
        ln2.next = new ListNode(2, true);
        ln2.next.next = ln2;

        System.out.println(s.hasCycle(ln2)); // [1, 2], pos 0 -> true

        ListNode ln3 = new ListNode(1);

        System.out.println(s.hasCycle(ln3)); // [1], pos -1 -> true
    }
}
