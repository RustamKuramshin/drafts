package com.rustam.dev.leetcode.p205;

import java.util.HashMap;
import java.util.Map;

public class Solution {
    public boolean isIsomorphic(String s, String t) {

        Map<Character, Character> map = new HashMap<>();

        for (int i = 0; i < s.length(); i++) {

            char key = s.charAt(i);

            Character c = map.get(key);

            char value = t.charAt(i);

            if (c == null) {
                if (map.containsValue(value)) {
                    return false;
                }
                map.put(key, value);
            } else {
                if (c.compareTo(value) != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void main(String[] args) {
        System.out.println((new Solution()).isIsomorphic("badc", "baba"));
    }
}
