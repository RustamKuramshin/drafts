package com.rustam.dev;

import java.util.HashMap;
import java.util.Map;

/**
 * Вам дан неупорядоченный массив целых чисел, в котором все элементы кроме одного имеют пару.
 * Реализуйте метод, который находит непарный элемент в массиве.
 */
public class FindSingle {

    /**
     * Медленное решение в лоб через вложенные циклы.
     * Временная сложность - O(n^2)
     * Пространственная сложность - O(1)
     */
    public static int findSingleWithLoops(int[] nums) {
        for (int i = 0; i < nums.length; i++) {
            boolean hasPair = false;
            for (int j = 0; j < nums.length; j++) {
                if (i != j && nums[i] == nums[j]) {
                    hasPair = true;
                    break;
                }
            }
            if (!hasPair) return nums[i];
        }
        throw new IllegalArgumentException("Непарный элемент не найден");
    }

    /**
     * Быстрое решение через HashMap, но с чрезмерным использованием памяти.
     * Временная сложность - O(n)
     * Пространственная сложность - O(n)
     */
    public static int findSingleWithMap(int[] nums) {
        Map<Integer, Integer> map = new HashMap<>();

        for (int num : nums) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }

        for (int num : nums) {
            if (map.get(num) == 1) {
                return num;
            }
        }

        throw new IllegalArgumentException("Непарный элемент не найден");
    }

    /**
     * Супер короткое и эффективное решение, если знать про "Исключающее ИЛИ" (он же XOR)
     * Временная сложность - O(n)
     * Пространственная сложность - O(1)
     */
    public static int findSingleWithXOR(int[] nums) {
        int result = 0;
        for (int num : nums) {
            result ^= num;
        }
        return result;
    }
}
