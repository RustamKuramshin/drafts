package com.rustam.dev.leetcode.treenode.p96;

class Solution {

    private int[] dp = new int[20];

    private int catalan(int n) {
        int res = 0;

        if (dp[n] != 0) return dp[n];

        if (n <= 1) {
            return 1;
        }

        for (int i = 0; i < n; i++) {
            res += catalan(i) * catalan(n - i - 1);
        }

        dp[n] = res;
        return res;
    }

    public int numTrees(int n) {
        return catalan(n);
    }
}