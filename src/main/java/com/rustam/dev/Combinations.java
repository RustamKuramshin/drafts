package com.rustam.dev;

import java.util.ArrayList;
import java.util.List;

public class Combinations {
    public List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> combinations = new ArrayList<>();
        backtrack(combinations, new ArrayList<>(), 1, n, k);
        return combinations;
    }

    private void backtrack(List<List<Integer>> combinations, List<Integer> current, int start, int n, int k) {
        if (current.size() == k) {
            combinations.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i <= n; i++) {
            current.add(i);
            backtrack(combinations, current, i + 1, n, k);
            current.remove(current.size() - 1); // откат
        }
    }

    public static void main(String[] args) {
        Combinations solution = new Combinations();
        List<List<Integer>> result = solution.combine(4, 2);
        for (List<Integer> combination : result) {
            System.out.println(combination);
        }
    }
}