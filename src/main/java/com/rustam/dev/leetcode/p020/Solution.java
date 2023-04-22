package com.rustam.dev.leetcode.p020;

import java.util.ArrayDeque;
import java.util.Deque;

public class Solution {

    private boolean isPaired(char opening, char closing) {
        if (opening > closing) return false;
        if (Math.abs(opening - closing) == 1) return true;
        if (Math.abs(opening - closing) == 2) return true;
        return false;
    }

    public boolean isValid(String s) {

        Deque<Character> stack = new ArrayDeque<>();

        for (int i = 0; i < s.length(); i++) {
            char currChar = s.charAt(i);

            Character first = stack.peekFirst();

            if (first != null) {
                boolean paired = isPaired(first, currChar);
                if (paired) {
                    stack.removeFirst();
                } else {
                    stack.addFirst(currChar);
                }
            } else {
                stack.addFirst(currChar);
            }
        }

        return stack.size() == 0;
    }
}
