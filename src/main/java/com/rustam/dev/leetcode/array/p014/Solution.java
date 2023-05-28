package com.rustam.dev.leetcode.array.p014;

public class Solution {
    public String longestCommonPrefix(String[] strs) {

        StringBuilder res = new StringBuilder();

        for (int i = 0; true; i++) {
            try {
                Character c = null;
                boolean flag = true;
                for (String str : strs) {
                    if (c == null) {
                        c = str.charAt(i);
                    } else {
                        if (c != str.charAt(i)) {
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    res.append(c);
                } else {
                    break;
                }
            } catch (Exception e) {
                break;
            }
        }
        return res.toString();
    }

    public static void main(String[] args) {
        System.out.println(new Solution().longestCommonPrefix(new String[]{"flower","flow","flight"}));
    }
}
