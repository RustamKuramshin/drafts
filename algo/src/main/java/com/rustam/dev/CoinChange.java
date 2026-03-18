package com.rustam.dev;

import java.util.Arrays;

public class CoinChange {

    public static void makeChange(int[] coins, int amount) {
        // Сортируем массив монет в порядке возрастания
        Arrays.sort(coins);
        int size = coins.length;

        // Массив для хранения количества монет каждого номинала
        int[] count = new int[size];

        // Проходим по массиву монет с конца (начиная с самого большого номинала)
        for (int i = size - 1; i >= 0; i--) {
            // Вычисляем максимальное количество монет текущего номинала
            count[i] = amount / coins[i];
            // Уменьшаем оставшуюся сумму
            amount -= count[i] * coins[i];
        }

        // Выводим результат
        System.out.println("Coin change:");
        for (int i = size - 1; i >= 0; i--) {
            if (count[i] != 0) {
                System.out.println(coins[i] + " x " + count[i]);
            }
        }
    }

    public static void main(String[] args) {
        int[] coins = {1, 5, 10, 25}; // Номиналы монет
        int amount = 63; // Сумма для размена

        makeChange(coins, amount);
    }
}

