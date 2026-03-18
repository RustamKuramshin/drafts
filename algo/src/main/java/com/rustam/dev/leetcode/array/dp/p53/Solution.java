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

    // recursive
    private int maxSum = Integer.MIN_VALUE;

    private void recursiveSum(int[] nums, int i, int sum, boolean inSubArr) {
        if (i >= nums.length) return;

        if (inSubArr) {
            sum += nums[i];
            if (sum > maxSum) maxSum = sum;
            recursiveSum(nums, i + 1, sum, true);
        } else {
            recursiveSum(nums, i, 0, true);
            recursiveSum(nums, i + 1, 0, false);
        }
    }

    public int maxSubArray2(int[] nums) {
        if (nums.length == 1) return nums[0];
        recursiveSum(nums, 0, 0, false);
        return maxSum;
    }

    public static void main(String[] args) {

        Solution s = new Solution();

//        int[] arr = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        int[] arr = {5, 4, -1, 7, 8};

        int maxSum = s.maxSubArray2(arr);

        System.out.println(maxSum);
    }
}
