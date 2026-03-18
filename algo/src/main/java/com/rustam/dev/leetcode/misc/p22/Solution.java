package com.rustam.dev.leetcode.misc.p22;

import java.util.ArrayList;
import java.util.List;

// https://leetcode.com/problems/generate-parentheses/
public class Solution {

    private List<String> combine(int n) {
        List<String> combinations = new ArrayList<>();
        backtrack(combinations, new StringBuilder(), n, 0, 0);
        return combinations;
    }

    private void backtrack(List<String> combinations, StringBuilder current, int n, int l, int r) {

        if (r > 0 && l == 0) return;

        if (r > l) return;

        if (current.length() == 2*n) {
            combinations.add(current.toString());
            return;
        }

        if (l < n) {
            current.append("(");
            backtrack(combinations, current, n, l + 1, r);
            current.deleteCharAt(current.length() - 1);
        }

        if (r < n) {
            current.append(")");
            backtrack(combinations, current, n, l, r + 1);
            current.deleteCharAt(current.length() - 1);
        }
    }

    public List<String> generateParenthesis(int n) {
        return combine(n);
    }

    public static void main(String[] args) {
        Solution s = new Solution();
        var res = s.generateParenthesis(3);
        System.out.println(res);
    }
}
