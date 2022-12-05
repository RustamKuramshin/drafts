package com.rustam.dev.leetcode.p205;

public class Solution {

    public boolean isIsomorphic(String s, String t) {

        char[] mainMap = new char[256];
        char[] secondMap = new char[256];

        for (int i = 0; i < s.length(); i++) {

            char key = s.charAt(i);
            char value = t.charAt(i);

            char c = mainMap[key];

            if (c == '\u0000') {
                if (secondMap[value] != '\u0000') {
                    return false;
                }
                mainMap[key] = value;
                secondMap[value] = key;
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
