package com.rustam.dev.leetcode.p448;

import java.util.ArrayList;
import java.util.List;

class Solution {

    private static void shift(int num, int[] nums) {
        int n = nums[num - 1];
        if (n == num) return;
        nums[num - 1] = num;
        if (n != -1) {
            shift(n, nums);
        }
    }

    public List<Integer> findDisappearedNumbers(int[] nums) {

        for (int i = 0; i < nums.length; i++) {
            int num = nums[i];
            if (num != i + 1) {
                nums[i] = -1;
                shift(num, nums);
            }
        }

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] == -1) {
                result.add(i + 1);
            }
        }
        return result;
    }

    public static void main(String[] args) {

        Solution s = new Solution();

        System.out.println(s.findDisappearedNumbers(new int[]{1, 2, 3, 4, 5, 6, 7, 8})); // []

        System.out.println(s.findDisappearedNumbers(new int[]{4, 3, 2, 7, 8, 2, 3, 1})); // [5, 6]

        System.out.println(s.findDisappearedNumbers(new int[]{1, 1})); // [2]

        System.out.println(s.findDisappearedNumbers(new int[]{1, 1, 1, 1})); // [2, 3, 4]

        System.out.println(s.findDisappearedNumbers(new int[]{1, 1, 1, 2})); // [3, 4]

        System.out.println(s.findDisappearedNumbers(new int[]{2, 1, 1, 1})); // [3, 4]
    }
}
