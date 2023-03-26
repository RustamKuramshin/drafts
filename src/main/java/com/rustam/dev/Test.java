package com.rustam.dev;

import java.util.Arrays;

class Test {

    private static class Node {

        int key = -1;
        int val = -1;
        int useCounter = 0;

        Node prev;
        Node next;

        Node(int val) {
            this.val = val;
        }

        Node(int key, int val) {
            this.key = key;
            this.val = val;
        }
    }

    public static void main(String[] args) {
        Node[] arr = new Node[10];

        System.out.println(Arrays.toString(arr));
    }
}