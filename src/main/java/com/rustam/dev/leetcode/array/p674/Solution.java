package com.rustam.dev.leetcode.array.p674;

public class Solution {

    public int findLengthOfLCIS(int[] nums) {

        // Первый особый случай
        if (nums.length == 1) return 1;

        int l = 0, r = 0, max = 0;

        for (int i = 0; i < nums.length - 1; i++) {

            if (nums[i] < nums[i + 1]) {
                r++;
            } else {
                if (max < r - l) max = r - l;
                l = r = i + 1;
            }
        }

        // Второй особый случай
        if (r == nums.length - 1 && nums[nums.length - 1] > nums[nums.length - 2]) {
            if (max < r - l) max = r - l;
        }

        return max + 1;
    }

    public static void main(String[] args) {

        Solution s = new Solution();

        System.out.println(s.findLengthOfLCIS(new int[]{1, 3, 5, 4, 7})); // 3
        System.out.println(s.findLengthOfLCIS(new int[]{1, 3, 5, 6, 7})); // 5
        System.out.println(s.findLengthOfLCIS(new int[]{1, 3, 5, 6, 1})); // 4
        System.out.println(s.findLengthOfLCIS(new int[]{2, 2, 2, 2, 2})); // 1
        System.out.println(s.findLengthOfLCIS(new int[]{2})); // 1
        System.out.println(s.findLengthOfLCIS(new int[]{2, 2})); // 1
        System.out.println(s.findLengthOfLCIS(new int[]{1, 2})); // 2
        System.out.println(s.findLengthOfLCIS(new int[]{2, 2, 2, 2, 2, 1, 2, 3, 4})); // 4
        System.out.println(s.findLengthOfLCIS(new int[]{2, 2, 2, 2, 2, 1, 2, 3, 4, 0, 1, 2, 3, 4, 5})); // 6
        System.out.println(s.findLengthOfLCIS(new int[]{2, -9, -8, -7, -6, -5, -4, -3, 4, 0, 1, 2, 3, 4, 5})); // 8
    }
}
