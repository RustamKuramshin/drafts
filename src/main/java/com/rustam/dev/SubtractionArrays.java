package com.rustam.dev;

import java.util.*;

public class SubtractionArrays {

    // return a - aANDb
    public static List<Integer> subtract(List<Integer> a, List<Integer> b) {

        List<Integer> res = new ArrayList<>();
        Iterator<Integer> aIter = a.iterator();
        Iterator<Integer> bIter = b.iterator();

        while (aIter.hasNext()) {
            Integer aItem = aIter.next();
            try {
                Integer bItem = -1;
                while (bItem < aItem) {
                    bItem = bIter.next();
                }

                if (bItem == aItem) {

                } else if (bItem > aItem) {
                    res.add(aItem);
                }

            } catch (NoSuchElementException e) {
                res.add(aItem);
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
