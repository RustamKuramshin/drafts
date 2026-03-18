package com.rustam.dev;

import java.util.Arrays;

public class SecondItems {

    private static int[] removeSecondItems(int[] arr) {

        int[] res = new int[arr.length/2 + arr.length%2];

        for (int i = 0, k = 0; i < arr.length; i=i+2) {
            res[k] = arr[i];
            k++;
        }

        return res;
    }

    public static void main(String[] args) {
        int[] test = new int[] {1, 2, 3};

        System.out.println(Arrays.toString(test));
        System.out.println(Arrays.toString(removeSecondItems(test)));
    }

}
