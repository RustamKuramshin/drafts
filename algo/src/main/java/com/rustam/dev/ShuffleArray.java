package com.rustam.dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Реализуйте метод "перемешивания" (shuffle) элементов массива.
 */
public class ShuffleArray {

    /**
     * Простой метод (на основе Fisher-Yates shuffle или Knuth shuffle).
     * Этот метод имеет временную сложность O(n)
     */
    private static int[] shuffleArray1(int[] arr) {
        Random rand = new Random();
        for (int i = arr.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);

            int temp = arr[i];
            arr[i] = arr[index];
            arr[index] = temp;
        }
        return arr;
    }

    /**
     * Используя дополнительный список и случайное извлечение элементов.
     * Этот метод имеет временную сложность O(n^2) из-за множественных операций удаления из списка.
     */
    private static int[] shuffleArray2(int[] arr) {
        Random rand = new Random();
        List<Integer> list = new ArrayList<>();
        for (int num : arr) {
            list.add(num);
        }

        int[] shuffled = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            int index = rand.nextInt(list.size());
            shuffled[i] = list.get(index);
            list.remove(index);
        }
        return shuffled;
    }

    /**
     * Используя Java Stream API.
     * Это более функциональный подход с использованием Java Stream API.
     * Он также использует дополнительное пространство для временного списка,
     * но его временная сложность составляет O(nlog(n)),
     * если считать, что операция sorted() на самом деле занимает O(nlog(n)) времени (например,
     * если она реализована через сортировку слиянием).
     */
    private static int[] shuffleArray3(int[] arr) {
        Random rand = new Random();
        return IntStream.range(0, arr.length)
                .boxed()
                .sorted((i, j) -> rand.nextInt(3) - 1)
                .mapToInt(i -> arr[i])
                .toArray();
    }

    public static void main(String[] args) {

        int[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        System.out.println(Arrays.toString(shuffleArray1(arr)));
        System.out.println(Arrays.toString(shuffleArray2(arr)));
        System.out.println(Arrays.toString(shuffleArray3(arr)));
    }
}
