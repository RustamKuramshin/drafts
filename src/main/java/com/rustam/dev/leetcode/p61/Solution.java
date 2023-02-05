package com.rustam.dev.leetcode.p61;

import java.util.ArrayList;
import java.util.List;

public class Solution {

    private static class ListNode {

        int val;

        ListNode next;

        ListNode() {
        }

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    private static void printListNode(ListNode listNode) {

        if (listNode == null) {
            System.out.println("[]");
            return;
        }

        StringBuilder res = new StringBuilder();
        res.append("[");

        ListNode node = listNode;
        do {
            res.append(node.val);
            node = node.next;
            if (node != null) res.append(", ");
        } while (node != null);

        res.append("]");

        System.out.println(res);
    }

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

        printListNode(s.rotateRight(ln1, 2));

        ListNode ln2 = new ListNode(0);
        ln2.next = new ListNode(1);
        ln2.next.next = new ListNode(2);

        printListNode(s.rotateRight(ln2, 4));

        ListNode ln3 = new ListNode(1);
        ln3.next = new ListNode(2);

        printListNode(s.rotateRight(ln3, 3));
    }
}
