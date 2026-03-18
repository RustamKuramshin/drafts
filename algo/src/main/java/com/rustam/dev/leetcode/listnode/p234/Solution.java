package com.rustam.dev.leetcode.listnode.p234;

import java.util.HashMap;
import java.util.Map;

import static com.rustam.dev.leetcode.LeetCodeUtils.ListNode;

// https://leetcode.com/problems/palindrome-linked-list/description/
public class Solution {

    public boolean isPalindrome(ListNode head) {

        Map<Integer, Object> counters = new HashMap<>();

        ListNode node = head;

        while (node != null) {

            if (counters.containsKey(node.val)) {
                counters.remove(node.val);
            } else {
                counters.put(node.val, null);
            }

            node = node.next;
        }

        return counters.size() <= 1;
    }
}
