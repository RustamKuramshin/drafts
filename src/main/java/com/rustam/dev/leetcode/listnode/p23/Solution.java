package com.rustam.dev.leetcode.listnode.p23;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

import static com.rustam.dev.leetcode.LeetCodeUtils.*;

// https://leetcode.com/problems/merge-k-sorted-lists/
public class Solution {

    public ListNode mergeKLists(ListNode[] lists) {

        if (lists.length == 0) return null;

        if (lists.length == 1) return lists[0];

        ListNode res = new ListNode();

        PriorityQueue<ListNode> minHeap = new PriorityQueue<>(lists.length, Comparator.comparingInt(l -> l.val));

        minHeap.addAll(Arrays.stream(lists).filter(Objects::nonNull).toList());

        if (minHeap.isEmpty()) return null;

        ListNode rnext = res;
        while (!minHeap.isEmpty()) {

            ListNode min = minHeap.poll();
            rnext.val = min.val;

            if (min.next != null) {
                minHeap.add(min.next);
            }

            if (!minHeap.isEmpty()) {
                rnext.next = new ListNode();
                rnext = rnext.next;
            }
        }

        return res;
    }
}
