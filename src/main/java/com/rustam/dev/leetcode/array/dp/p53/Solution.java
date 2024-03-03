package com.rustam.dev.leetcode.array.dp.p53;

// https://leetcode.com/problems/maximum-subarray/description/
public class Solution {

    // greedy
    public int maxSubArray1(int[] nums) {

        if (nums.length == 1) return nums[0];

        int maxSum = Integer.MIN_VALUE;

        for (int i = 0; i < nums.length; i++) {

            int sum = 0;
            sum += nums[i];
            if (sum > maxSum) maxSum = sum;

            for (int j = i + 1; j < nums.length; j++) {
                sum += nums[j];
                if (sum > maxSum) maxSum = sum;
            }
        }

        return maxSum;
    }

    public int maxSubArray2(int[] nums) {
        return 0;
    }
}
