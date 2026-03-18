package com.rustam.dev;

import java.util.HashMap;
import java.util.Map;

/**
 * Дан неупорядоченный массив целых чисел.
 * Реализуйте метод, выполняющий поиск двух чисел в массиве, которые в сумме дают заданное число.
 * В ответ верните два индекса этих чисел в массиве.
 */
public class TwoSum {

    /**
     * Не эффективное решение грубой силой с вложенными циклами.
     * Временная сложность - O(n^2)
     * Пространственная сложность - O(1)
     */
    public static int[] twoSumWithLoops(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[]{i, j};
                }
            }
        }

        throw new IllegalArgumentException("Не удалось найти требуемые элементы");
    }

    /**
     * Быстрое решение через HashMap.
     * Временная сложность - O(n)
     * Пространственная сложность - O(n)
     */
    public static int[] twoSumWithMap(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[]{map.get(complement), i};
            }
            map.put(nums[i], i);
        }
        throw new IllegalArgumentException("Не удалось найти требуемые элементы");
    }
}
