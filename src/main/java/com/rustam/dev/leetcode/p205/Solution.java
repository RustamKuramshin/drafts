package com.rustam.dev.leetcode.p205;

public class Solution {

    private boolean contains(char[] arr, char f) {
        for (char c : arr) {
            if (c == f) return true;
        }
        return false;
    }

    public boolean isIsomorphic(String s, String t) {

        char[] map = new char[256];

        for (int i = 0; i < s.length(); i++) {

            char key = s.charAt(i);
            char value = t.charAt(i);

            char c = map[key];

            if (c == '\u0000') {
                if (contains(map, value)) {
                    return false;
                }
                map[key] = value;
            } else {
                if (c != value) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void main(String[] args) {

        var solution = new Solution();

        System.out.println(solution.isIsomorphic("egg", "add"));
        System.out.println(solution.isIsomorphic("foo", "bar"));
        System.out.println(solution.isIsomorphic("paper", "title"));
        System.out.println(solution.isIsomorphic("badc", "baba"));
    }
}
