package com.rustam.dev;

import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Collectors;

public class StreamTest {

    public static void main(String[] args) {

        var integerList = new ArrayList<Integer>();

        for (var j = 0; j < 100000; j++) {
            var randInt = new Random().nextInt(100000 - 1000) + 1000;
            integerList.add((randInt % 2 == 0) ? -1 * randInt : randInt);
        }

        var startTime = System.nanoTime();

        var filteredList = integerList.stream()
                .map(i -> i * 3)
                .filter(i -> i > 0)
                .collect(Collectors.toList());

        System.out.println("Выполнение заняло нс: " + (System.nanoTime() - startTime));

        System.out.println("Отфильтровано элементов: " + filteredList.size());
    }
}
