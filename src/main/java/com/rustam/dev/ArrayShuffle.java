package com.rustam.dev;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ArrayShuffle {

    private static int[] shuffleArray1(int[] arr) {

        List<Integer> intList = Arrays.stream(arr)
                .boxed()
                .collect(Collectors.toList());

        Collections.shuffle(intList);

        return intList.stream().mapToInt(i -> i).toArray();
    }

    private static int[] shuffleArray2(int[] array) {

        Random rand = new Random();

        for (int i = 0; i < array.length; i++) {
            int randomIndexToSwap = rand.nextInt(array.length);
            int temp = array[randomIndexToSwap];
            array[randomIndexToSwap] = array[i];
            array[i] = temp;
        }
        return array;
    }

    public static void main(String[] args) {

        int[] arr = {1, 2, 3, 4, 5, 6 ,7, 8,9 };

        System.out.println(Arrays.toString(shuffleArray1(arr)));

        System.out.println(Arrays.toString(shuffleArray2(arr)));
    }
}
