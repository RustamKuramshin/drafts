package com.rustam.dev;

import java.util.TreeMap;

class Test {
    public static void main(String[] args) {

        TreeMap<Integer, String> tm = new TreeMap<>();

        tm.put(10, "aer");
        tm.put(123, "aer");
        tm.put(1, "aer");
        tm.put(4564, "aer");
        tm.put(3456, "aer");
        tm.put(4, "aer");

        System.out.println(tm);

        tm.remove(1);

        System.out.println(tm);

    }
}

