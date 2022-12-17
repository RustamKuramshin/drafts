package com.rustam.dev.replit.unpairedsearch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


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
        System.out.println("Run unpaired search test");
        org.junit.runner.JUnitCore.main("Main");
    }

    @Test
    public void testFindItemWithoutPair_1() {
        int[] arr1 = {2, 6, 3, 9, 2, 3, 6};
        assertEquals(9, Main.findItemWithoutPair(arr1));
    }

    @Test
    public void testFindItemWithoutPair_2() {
        int[] arr1 = {4, 1, 1, 2, 2, 3, 3};
        assertEquals(4, Main.findItemWithoutPair(arr1));
    }

    @Test
    public void testFindItemWithoutPair_3() {
        int[] arr1 = {10};
        assertEquals(10, Main.findItemWithoutPair(arr1));
    }

    @Test
    public void testFindItemWithoutPair_4() {
        int[] arr1 = {1, 0, 1};
        assertEquals(0, Main.findItemWithoutPair(arr1));
    }
}
