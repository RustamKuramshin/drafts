package com.rustam.dev;

import java.util.HashMap;
import java.util.Map;

/**
 * Дан массив целых чисел.
 * Напишите функцию, которая возвращает число,
 * которое встречается в массиве наибольшее количество раз.
 * Если таких чисел несколько, верните наименьшее из них.
 */
public class MostCommon {

    public int mostCommon(int[] nums) {
        Map<Integer, Integer> countMap = new HashMap<>();
        for (int num : nums) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        int maxCount = -1;
        int mostCommonNum = Integer.MAX_VALUE;

        for (Map.Entry<Integer, Integer> entry : countMap.entrySet()) {
            if (entry.getValue() > maxCount || (entry.getValue() == maxCount && entry.getKey() < mostCommonNum)) {
                maxCount = entry.getValue();
                mostCommonNum = entry.getKey();
            }
        }
        return mostCommonNum;
    }

}
