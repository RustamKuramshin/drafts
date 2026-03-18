package com.rustam.dev.replit.unpairedsearch;

public class Main {
    /*Метод принимает массив int'ов, в котором каждый элемент
    кроме одного встречается дважды. Надо найти элемент без пары.*/
    public static int findItemWithoutPair(int[] arr) {
        // TODO: здесь написать реализацию

        // (a xor b) xor b = a

        int res = 0;
        for (int item : arr) {
            res = item ^ res;
        }
        return res;
    }

    public static void main(String[] args) {
        int[] arr = {2, 6, 3, 9, 2, 3, 6};
        System.out.println(findItemWithoutPair(arr));
    }
}
