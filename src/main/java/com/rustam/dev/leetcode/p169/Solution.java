package com.rustam.dev.leetcode.p169;

import java.util.HashMap;

public class Solution {

    public int majorityElement(int[] nums) {

        if (nums.length == 1) return nums[0];

        int target = nums.length/2;

        HashMap<Integer, Integer> hm = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            Integer currCounter = hm.get(nums[i]);

            if (currCounter == null) {
                hm.put(nums[i], 1);
            } else {
                if ((currCounter + 1) > target) return nums[i];
                hm.put(nums[i], currCounter + 1);
            }
        }

        return -1;
    }

    public static void main(String[] args) {
        Solution s = new Solution();

        int[] arr1 = {3, 2, 3};

        System.out.println(s.majorityElement(arr1));

        int[] arr2 = {2, 2, 1, 1, 1, 2, 2};

        System.out.println(s.majorityElement(arr2));
    }
}
