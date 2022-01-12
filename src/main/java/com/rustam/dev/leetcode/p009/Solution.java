package com.rustam.dev.leetcode.p009;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Solution {
    public boolean isPalindrome(int x) {

        if (x < 0) {
            return false;
        }

        boolean res = true;

        List<Integer> digits = new ArrayList<>();

        for (int num = x; num > 0; num /= 10) {
            digits.add(num%10);
        }

        List<Integer> clone = new ArrayList<>(digits);
        Collections.reverse(clone);

        for (int i = 0; i < digits.size(); i++) {

            if (!Objects.equals(digits.get(i), clone.get(i))) {
                res = false;
                break;
            }

        }

        return res;
    }

    public static void main(String[] args) {
        System.out.println(new Solution().isPalindrome(121));
    }
}
