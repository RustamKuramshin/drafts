package com.rustam.dev.stepik.javabasic;

import java.util.Arrays;

public class S24T2 {
    public static int[] mergeArrays(int[] a1, int[] a2) {

        int[] res = new int[a1.length + a2.length];

        for(int i = 0, j = 0, k = 0; i < res.length; i++) {
            Integer a1Item = (j < a1.length) ? a1[j] : null;
            Integer a2Item = (k < a2.length) ? a2[k] : null;

            if (a1Item == null && a2Item != null) {
                res[i] = a2Item;
                k++;
                continue;
            } else if(a2Item == null && a1Item != null) {
                res[i] = a1Item;
                j++;
                continue;
            } else if (a1Item == null && a2Item == null) {
                break;
            }

            if (a1Item <= a2Item) {
                res[i] = a1Item;
                j++;
            } else {
                res[i] = a2Item;
                k++;
            }
        }

        return res;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(mergeArrays(new int[]{1}, new int[]{2})));
    }
}
