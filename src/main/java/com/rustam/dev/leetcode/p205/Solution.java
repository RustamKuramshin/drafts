package com.rustam.dev.leetcode.p205;

public class Solution {

    public static boolean equals(final String s1, final String s2) {
        return s1 != null && s2 != null && s1.hashCode() == s2.hashCode() && s1.equals(s2);
    }

    public boolean isIsomorphic(String s, String t) {

        if (!equals(s, t)) {

            byte[] map = new byte[254];

            for (int i = 0; i < s.length(); i++) {

                byte key = (byte) s.charAt(i);
                byte value = (byte) t.charAt(i);

                byte c = map[key];

                if (c == 0) {
                    if (map[value + 127] != 0) {
                        return false;
                    }
                    map[key] = value;
                    map[value + 127] = key;
                } else {
                    if (c != value) {
                        return false;
                    }
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
