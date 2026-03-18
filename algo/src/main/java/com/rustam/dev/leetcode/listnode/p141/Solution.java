package com.rustam.dev.leetcode.listnode.p141;

import java.util.HashSet;
import java.util.Set;

import static com.rustam.dev.leetcode.LeetCodeUtils.ListNode;

// https://leetcode.com/problems/linked-list-cycle/
public class Solution {

    public boolean hasCycle(ListNode head) {
        head.printListNode();

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
        ln1.next.next.next = new ListNode(-4);
        ln1.next.next.next.next = ln1.next;

        System.out.println(s.hasCycle(ln1)); // [3, 2, 0, -4], pos = 1 -> true

        ListNode ln2 = new ListNode(1);
        ln2.next = new ListNode(2);
        ln2.next.next = ln2;

        System.out.println(s.hasCycle(ln2)); // [1, 2], pos 0 -> true

        ListNode ln3 = new ListNode(1);

        System.out.println(s.hasCycle(ln3)); // [1], pos -1 -> true
    }
}
