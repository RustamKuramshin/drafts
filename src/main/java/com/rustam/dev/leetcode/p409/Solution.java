package com.rustam.dev.leetcode.p409;

class Solution {

    public int longestPalindrome(String s) {

        if (s.length() == 1) return 1;

        short[] charRegistry = new short[58];
        int result = 0;
        int singletonCount = 0;

        for (int i = 0; i < s.length(); i++) {

            byte sChar = (byte) s.charAt(i);
            byte sCharIdx = (byte) (sChar - 65);

            short sCharCount = charRegistry[sCharIdx];
            short newSCharCount = (short) (sCharCount + 1);

            if (newSCharCount == 1) {
                charRegistry[sCharIdx] = newSCharCount;
                singletonCount++;
            } else if (newSCharCount == 2) {
                result = result + 2;
                charRegistry[sCharIdx] = 0;
                singletonCount--;
            }
        }

        if (singletonCount > 0) {
            result = result + 1;
        }

        return result;
    }

    public static void main(String[] args) {

        var s = new Solution();

        System.out.println(s.longestPalindrome("abccccdd")); // 7
        System.out.println(s.longestPalindrome("a")); // 1
        System.out.println(s.longestPalindrome("Z")); // 1
        System.out.println(s.longestPalindrome("aaa")); // 3
        System.out.println(s.longestPalindrome("aaaa")); // 4
        System.out.println(s.longestPalindrome("aA")); // 1
        System.out.println(s.longestPalindrome("aAA")); // 3
        System.out.println(s.longestPalindrome("aAAA")); // 3
        System.out.println(s.longestPalindrome("aba")); // 3
    }
}
