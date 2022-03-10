package com.rustam.dev.leetcode.p001;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Solution {
    public int[] twoSum(int[] nums, int target) {
        int[] res = {-1, -1};
        Map<Integer, Integer> pairs = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int sub = target - nums[i];
                if (pairs.containsKey(sub)) {
                    res[0] = pairs.get(sub);
                    res[1] = i;
                } else {
                    pairs.put(nums[i], i);
                }
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(new Solution().twoSum(new int[]{0,4,3,0}, 0)));
    }
}
