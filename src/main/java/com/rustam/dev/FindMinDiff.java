package com.rustam.dev;

import java.util.Map;
import java.util.TreeMap;

public class FindMinDiff {

    public static int findMinDiff(int[] arr) {

        TreeMap<Integer, Integer> mp = new TreeMap<>();

        int minDiff = Integer.MAX_VALUE;

        for (int i = 0; i < arr.length; i++) {

            Map.Entry<Integer, Integer> entry = mp.ceilingEntry(arr[i]);

            if (entry != null) {
                minDiff = Math.min(minDiff, entry.getKey() - arr[i]);
            }

            entry = mp.lowerEntry(arr[i]);

            if (entry != null) {
                minDiff = Math.min(minDiff, arr[i] - entry.getKey());
            }

            mp.put(arr[i], i);
        }

        return minDiff;
    }
}
