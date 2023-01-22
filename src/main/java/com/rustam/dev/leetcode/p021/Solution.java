package com.rustam.dev.leetcode.p021;

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

    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {

        if (list1 == null && list2 == null) return null;
        if (list1 == null) return list2;
        if (list2 == null) return list1;

        ListNode node1 = list1;
        ListNode node2 = list2;

        do {

            if (node1 == null) {
//                ListNode tmp = node2.next;
//                if (tmp != null) {
//                    if (node2.val > tmp.val) {
//
//                    }
//                }

                break;
            }

            if (node1.val == node2.val) {
                ListNode tmp = node2.next;
                node2.next = new ListNode(node1.val);
                node2.next.next = tmp;
                node2 = node2.next;

                node1 = node1.next;
                node2 = node2.next;
                continue;
            }

            if (node1.val < node2.val) {
                int tmpVal = node2.val;
                ListNode tmp = node2.next;
                node2.val = node1.val;
                node2.next = new ListNode(tmpVal);
                node2.next.next = tmp;

                node1 = node1.next;
                node2 = node2.next;
                continue;
            }

            if (node1.val > node2.val) {
                if (node2.next != null) {
                    if (node1.val > node2.next.val) {
                        node2 = node2.next;
                        continue;
                    }
                }

                ListNode tmp = node2.next;
                node2.next = new ListNode(node1.val);
                node2.next.next = tmp;

                node1 = node1.next;
                node2 = node2.next;
            }

        } while (node1 != null || node2 != null);


        return list2;
    }

    public static void main(String[] args) {

        Solution s = new Solution();

//        ListNode ln1 = new ListNode(1);
//        ln1.next = new ListNode(2);
//        ln1.next.next = new  ListNode(4);
//
//        ListNode ln2 = new ListNode(1);
//        ln2.next = new ListNode(3);
//        ln2.next.next = new ListNode(4);
//
//        printListNode(s.mergeTwoLists(ln1, ln2)); // [1, 1, 2, 3, 4, 4]
//
//        printListNode(s.mergeTwoLists(null, null)); // null
//
//        ListNode ln6 = new ListNode(0);
//
//        printListNode(s.mergeTwoLists(null, ln6)); // [0]
//
//
//        ListNode ln7 = new ListNode(1);
//        ln7.next = new ListNode(1);
//        ln7.next.next = new  ListNode(1);
//
//        ListNode ln8 = new ListNode(2);
//        ln8.next = new ListNode(2);
//        ln8.next.next = new ListNode(2);
//
//        printListNode(s.mergeTwoLists(ln7, ln8)); // [1, 1, 1, 2, 2, 2]
//
//        ListNode ln9 = new ListNode(1);
//
//        ListNode ln10 = new ListNode(2);
//
//        printListNode(s.mergeTwoLists(ln9, ln10)); // [1, 2]
//
//        ListNode ln11 = new ListNode(1);
//
//        ListNode ln12 = new ListNode(1);
//
//        printListNode(s.mergeTwoLists(ln11, ln12)); // [1, 1]
//
//        ListNode ln13 = new ListNode(1);
//
//        ListNode ln14 = new ListNode(0);
//
//        printListNode(s.mergeTwoLists(ln13, ln14)); // [0, 1]
//
//        ListNode ln15 = new ListNode(5);
//
//        ListNode ln16 = new ListNode(1);
//        ln16.next = new ListNode(2);
//        ln16.next.next = new  ListNode(4);
//
//        printListNode(s.mergeTwoLists(ln15, ln16)); // [1, 2, 4, 5]
//
//        ListNode ln17 = new ListNode(5);
//        ln17.next = new ListNode(6);
//
//        ListNode ln18 = new ListNode(1);
//        ln18.next = new ListNode(2);
//        ln18.next.next = new  ListNode(4);
//
//        printListNode(s.mergeTwoLists(ln17, ln18)); // [1, 2, 4, 5, 6]

        ListNode ln19 = new ListNode(-6);
        ln19.next = new ListNode(-5);
        ln19.next.next = new ListNode(6);
        ln19.next.next.next = new ListNode(6);
        ln19.next.next.next.next = new ListNode(7);

        ListNode ln20 = new ListNode(0);

        printListNode(s.mergeTwoLists(ln19, ln20)); // [-6, -5, 0, 6, 6, 7]

    }
}




