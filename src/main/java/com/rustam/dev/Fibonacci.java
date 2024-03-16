package com.rustam.dev;

public class Fibonacci {

    public static void printFibonacciNumbers(int n) {
        if (n < 1) {
            return;
        }

        long first = 0, second = 1;

        for (int i = 1; i <= n; ++i) {
            System.out.print(first + " ");

            long next = first + second;
            first = second;
            second = next;
        }
    }

    public static void printFibonacciNumbers2(int n) {
        for (int i = 0; i < n; i++) {
            System.out.print(fibonacci(i) + " ");
        }
    }

    public static long fibonacci(int n) {
        if (n <= 1) {
            return n;
        } else {
            return fibonacci(n - 1) + fibonacci(n - 2);
        }
    }

    public static void main(String[] args) {
        int n = 10; // Например, печатаем первые 10 чисел Фибоначчи
        printFibonacciNumbers(n);
    }
}

