package com.rustam.dev;

/**
 * Напишите функцию, которая принимает целое число и возвращает его "обратное" число.
 * Не используйте строковые операции для решения этой задачи.
 */
public class ReverseNumber {

    public int reverseNumber(int num) {
        int reversed = 0;

        while (num != 0) {
            reversed = reversed * 10 + num % 10;
            num /= 10;
        }

        return reversed;
    }

}
