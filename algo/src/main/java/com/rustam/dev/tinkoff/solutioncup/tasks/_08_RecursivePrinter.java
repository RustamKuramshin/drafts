package com.rustam.dev.tinkoff.solutioncup.tasks;

/*
Рекурсивная печать

Текст задачи

На вход приходит n. Необходимо вывести в консоль числа от 1 до n, используя рекурсию.
Если n < 1 — ничего не выводить.

Ограничения

• n > 0;
• если n < 1, ничего не выводить;
• для вывода каждого числа использовать System.out.println.

Пример

n = 3
Вывод:
1
2
3

n = 1
Вывод:
1

n = 0
Ничего не выводить

n = -1
Ничего не выводить

Задача: реализовать рекурсивную функцию print(int n), которая выводит
все числа от 1 до n включительно, каждое на новой строке. Вызов функции
уже находится в методе main.
*/

import java.util.Scanner;

class _08_RecursivePrinter {
    public static void main(String[] args) {
        var scanner = new Scanner(System.in);
        var inputNumber = Integer.parseInt(scanner.nextLine());
        print(inputNumber);
    }

    public static void print(int n) {
        // реализуйте этот метод

    }
}
