package com.rustam.dev.leetcode.p205;

public class Solution {

    public static boolean equals(final String s1, final String s2) {
        return s1 != null && s2 != null && s1.hashCode() == s2.hashCode() && s1.equals(s2);
    }

    public boolean isIsomorphic(String s, String t) {

        char[] mainMap = new char[260];

        for (int i = 0; i < s.length(); i++) {

            char key = s.charAt(i);
            char value = t.charAt(i);

            char c = mainMap[key];

            if (c == '\u0000') {
                if (mainMap[value+130] != '\u0000') {
                    return false;
                }
                mainMap[key] = value;
                mainMap[value+130] = key;
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
