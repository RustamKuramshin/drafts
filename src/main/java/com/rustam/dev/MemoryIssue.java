package com.rustam.dev;

import java.util.ArrayList;
import java.util.List;

public class MemoryIssue {

    private static final Integer COUNT = 50_000_000;

    private List<Integer> integerList = new ArrayList<>();

    public void make() {

        for (int i = 0; i < COUNT; i++) {
            integerList.add(i);
        }

        for (int i = 0; i < COUNT; i++) {
            System.out.println(integerList.get(i));
        }
    }

    public static void main(String[] args) {
        MemoryIssue memoryIssue = new MemoryIssue();

        memoryIssue.make();
    }
}
