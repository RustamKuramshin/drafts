package com.rustam.dev;

import java.util.*;

public class SubtractionArrays {
    // вычесть общие элементы из массива A
    public static List<Integer> subtract(List<Integer> a, List<Integer> b) {

        List<Integer> res = new ArrayList<>();

        for (int ai = 0, bi = 0; ai < a.size(); ai++) {

            // пока массив B не закончился сравниваем элементы
            if (bi < b.size()) {

                // сматываем элементы меньше текущего
                if (b.get(bi) < a.get(ai)) {
                    while (b.get(bi) < a.get(ai)) {
                        bi++;
                        if (bi > b.size() - 1) {
                            res.add(a.get(ai));
                            break;
                        }
                    }
                }

                // если массив B закончился, идем дальше по массиву A
                if (bi > b.size() - 1) {
                    continue;
                }

                // пропускаем совпадающие элементы
                if (Objects.equals(b.get(bi), a.get(ai))) {
                    bi++;
                } else if (b.get(bi) > a.get(ai)) {
                    res.add(a.get(ai));
                }

            } else {
                res.add(a.get(ai));
            }
        }

        return res;
    }

    public static void main(String[] args) {
        List<Integer> a = Arrays.asList(1, 11, 15, 24, 27, 31, 43, 70, 93);
        List<Integer> b = Arrays.asList(0, 5, 8, 12, 13, 24, 25, 27, 31, 56, 63, 67);

        System.out.println(subtract(a, b));
    }
}
