package com.rustam.dev;

public class BinarySearchForStringArray {

    public static int findIndexOfElement(String[] array, String target) {
        int left = 0;
        int right = array.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            int res = target.compareTo(array[mid]);

            if (res == 0) return mid;

            if (res > 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

    public static void main(String[] args) {
        String[] array = {"apple", "banana", "cherry", "date", "fig", "grape"};

        System.out.println("Index of 'cherry': " + findIndexOfElement(array, "cherry"));
        System.out.println("Index of 'apple': " + findIndexOfElement(array, "apple"));
        System.out.println("Index of 'grape': " + findIndexOfElement(array, "grape"));
        System.out.println("Index of 'orange': " + findIndexOfElement(array, "orange"));
    }
}
