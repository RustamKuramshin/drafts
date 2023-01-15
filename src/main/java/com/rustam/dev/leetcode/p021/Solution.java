package com.rustam.dev.leetcode.p021;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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

        ListNodeIterator listNodeIterator = new ListNodeIterator(listNode);

        List<Integer> res = new ArrayList<>();

        while (listNodeIterator.hasNext()) {
            res.add(listNodeIterator.next());
        }

        System.out.println(res);
    }

    private static class ListNodeIterator {

        private ListNode lastReturned;
        private ListNode next;

        public ListNodeIterator(ListNode listNode) {
            this.next = listNode;
        }

        public boolean hasNext() {
            return next != null;
        }

        public int next() {

            if (!hasNext())
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            return lastReturned.val;
        }
    }

    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {

        ListNode result = null;
        ListNode tail = null;

        ListNodeIterator listNodeIterator1 = new ListNodeIterator(list1);
        ListNodeIterator listNodeIterator2 = new ListNodeIterator(list2);

        while (true) {

            if (!(listNodeIterator1.hasNext() || listNodeIterator2.hasNext())) break;

            Integer val1 = listNodeIterator1.hasNext() ? listNodeIterator1.next() : null;
            Integer val2 = listNodeIterator2.hasNext() ? listNodeIterator2.next(): null;

            ListNode n1 = new ListNode(val1);
            ListNode n2 = new ListNode(val2);

            if (val1 <= val2) {
                n1.next = n2;
            } else {
                n2.next = n1;
            }

            if (result == null) {
                result = val1 <= val2 ? n1 : n2;
                tail = result.next;
            } else {
                tail.next = val1 <= val2 ? n1 : n2;
                tail = tail.next;
            }

        }

        return result;
    }

    public static void main(String[] args) {

        Solution s = new Solution();

        ListNode ln1 = new ListNode(1);
        ln1.next = new ListNode(2);
        ln1.next.next = new  ListNode(4);

        ListNode ln2 = new ListNode(1);
        ln2.next = new ListNode(3);
        ln2.next.next = new ListNode(4);

        printListNode(s.mergeTwoLists(ln1, ln2)); // [1, 1, 2, 3, 4, 4]

        ln1 = null;
        ln2 = null;

        ln1 = new ListNode(1);
        ln1.next = new ListNode(1);
        ln1.next.next = new  ListNode(1);

        ln2 = new ListNode(2);
        ln2.next = new ListNode(2);
        ln2.next.next = new ListNode(2);

        printListNode(s.mergeTwoLists(ln1, ln2)); // [1, 1, 1, 2, 2, 2]

    }
}




