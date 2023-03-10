package com.rustam.dev.javarush.doublelinkedlist;

public class StringLinkedList {
    private Node first = new Node();
    private Node last = new Node();

    public static class Node {
        private Node prev;
        private Node next;
        private String value;

        public Node() {

        }

        public Node(String value) {
            this.value = value;
        }

    }

    public void printAll() {
        Node currentElement = first.next;
        while ((currentElement) != null && currentElement.value != null) {
            System.out.println(currentElement.value);
            currentElement = currentElement.next;
        }
    }

    public void add(String value) {
        Node newNode = new Node(value);
        if (isEmpty()) {
            first.next = newNode;
            newNode.prev = first;
            newNode.next = last;
            last.prev = newNode;
        } else {
            Node lastNode = last.prev;

            lastNode.next = newNode;

            newNode.prev = lastNode;
            newNode.next = last;

            last.prev = newNode;
        }
    }

    private boolean isEmpty() {
        return first.next == null && last.prev == null;
    }
}
