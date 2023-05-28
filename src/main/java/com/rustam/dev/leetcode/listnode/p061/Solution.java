package com.rustam.dev.leetcode.listnode.p061;

import java.util.ArrayList;
import java.util.List;

import static com.rustam.dev.leetcode.LeetCodeUtils.ListNode;

// https://leetcode.com/problems/rotate-list/
public class Solution {

    private static List<Integer> listNodeToList(ListNode head) {
        List<Integer> nodesList = new ArrayList<>();

        ListNode node = head;
        while (node != null) {
            nodesList.add(node.val);
            node = node.next;
        }

        return nodesList;
    }

    private static ListNode arrayToListNode(int[] arr) {
        ListNode listNode = new ListNode();
        ListNode node = listNode;
        for (int j = 0; j < arr.length; j++) {
            node.val = arr[j];
            if (j < arr.length - 1) {
                node.next = new ListNode();
            } else {
                break;
            }
            node = node.next;
        }

        return listNode;
    }

    public ListNode rotateRight(ListNode head, int k) {

        if (head == null) return null;

        List<Integer> nodesList = listNodeToList(head);
        int nodeListSize = nodesList.size();
        int nodeListLastIndex = nodeListSize - 1;

        int[] res = new int[nodeListSize];

        k = k%nodeListSize;

        for (int i = 0; i < nodeListSize; i++) {

            int nodeListItem = nodesList.get(i);

            int newIndex;

            if (i + k <= nodeListLastIndex) {
                newIndex = i + k;
            } else {
                newIndex = ((i + k) - nodeListLastIndex) - 1;
            }

            res[newIndex] = nodeListItem;
        }

        return arrayToListNode(res);
    }

    public static void main(String[] args) {

        Solution s = new Solution();

        ListNode ln1 = new ListNode(1);
        ln1.next = new ListNode(2);
        ln1.next.next = new ListNode(3);
        ln1.next.next.next = new ListNode(4);
        ln1.next.next.next.next = new ListNode(5);

        s.rotateRight(ln1, 2).printListNode();

        ListNode ln2 = new ListNode(0);
        ln2.next = new ListNode(1);
        ln2.next.next = new ListNode(2);

        s.rotateRight(ln2, 4).printListNode();

        ListNode ln3 = new ListNode(1);
        ln3.next = new ListNode(2);

        s.rotateRight(ln3, 3).printListNode();
    }
}
