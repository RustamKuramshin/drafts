package com.rustam.dev;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

/**
 * Дан массив интервалов, где intervals[i] = [starti, endi], объедините все пересекающиеся интервалы
 * и верните новый массив непересекающихся интервалов.
 * Пример:
 * Вход: intervals = [[1,3],[2,6],[8,10],[15,18]]
 * Выход: [[1,6],[8,10],[15,18]]
 */
public class MergeIntervals {

    public int[][] merge(int[][] intervals) {
        if (intervals.length <= 1)
            return intervals;

        // Сортируем интервалы по начальной границе
        Arrays.sort(intervals, Comparator.comparingInt(arr -> arr[0]));

        LinkedList<int[]> merged = new LinkedList<>();
        for (int[] interval : intervals) {
            // Если список результата пуст или не пересекается с предыдущим интервалом, добавляем
            if (merged.isEmpty() || merged.getLast()[1] < interval[0]) {
                merged.add(interval);
            }
            // В противном случае, объединяем с последним интервалом
            else {
                merged.getLast()[1] = Math.max(merged.getLast()[1], interval[1]);
            }
        }

        return merged.toArray(new int[merged.size()][]);
    }

}
