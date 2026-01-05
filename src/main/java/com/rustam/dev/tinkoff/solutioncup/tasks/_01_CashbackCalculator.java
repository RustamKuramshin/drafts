package com.rustam.dev.tinkoff.solutioncup.tasks;

/*
Калькулятор кэшбэка

Дан код, который должен поиндексно перемножать два массива одинаковой длины:
- первый массив содержит суммы покупок;
- второй массив содержит проценты начисляемого кэшбэка для каждой покупки.

Нужно посчитать итоговую сумму кэшбэка, которую должен получить покупатель, и вывести
её в стандартный поток вывода (System.out).

Даны вспомогательные данные и ограничения:
- Метод List<Double> TCupUtil.getDoubles(BufferedReader br) читает последовательность
  вещественных чисел из System.in и возвращает их в виде списка. Формат входных данных
  и реализация метода не важны для решения задачи.
- Сначала из входного потока читается список процентов кэшбэка, затем список сумм покупок.
- Гарантируется, что оба списка имеют одинаковую длину.
- Ответ должен быть выведен одним числом (double) в System.out.

Пример

Входные данные:
0.1;0.02
100;300

Пояснение:
Кэшбэк с первой покупки: 100 * 0.1 = 10
Кэшбэк со второй покупки: 300 * 0.02 = 6
Итоговая сумма кэшбэка: 16.0

Выходные данные:
16.0

Требуется написать/исправить код так, чтобы он корректно считал и выводил сумму кэшбэка.
*/

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.List;

public class _01_CashbackCalculator {

    public static void main(String[] args) throws IOException {
        BufferedReader inputDataBR = new BufferedReader(new InputStreamReader(System.in));

        // Список процентов кэшбэка
        List<Double> percents = List.of(0.1, 0.02);

        // Список сумм покупок
        List<Double> purchases = List.of(100.0, 300.0);

        // TODO: поиндексно перемножить элементы списков purchases и percents,
        //       просуммировать получившиеся значения и вывести сумму в System.out

        double sum = 0;
        for (int i = 0; i < purchases.size(); i++) {
            sum = sum + purchases.get(i) * percents.get(i+1);
        }
        System.out.println(sum);
    }
}
