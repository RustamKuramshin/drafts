package com.rustam.dev.tinkoff;

import java.util.Stack;

/**
 * Есть двери пронумерованные от 0 до N-1.
 * За каждой дверью находятся пронумерованные ключи, которые открывают другие двери.
 * В самом начале открыта только дверь с номером 0.
 * Нужно по входящему двумерному массиву понять сможем ли мы посетить все комнаты или нет.
 * В ответ вернуть true или false.
 */
public class RoomsVisiter {

    private static boolean canVisitAllRooms(int[][] rooms) {
        if (rooms == null || rooms.length == 0) return false;

        int n = rooms.length;
        boolean[] visited = new boolean[n];
        Stack<Integer> stack = new Stack<>();

        stack.push(0);

        while (!stack.isEmpty()) {
            int currentRoom = stack.pop();
            visited[currentRoom] = true;
            for (int key : rooms[currentRoom]) {
                if (!visited[key]) {
                    stack.push(key);
                }
            }
        }

        for (boolean v : visited) {
            if (!v) return false;
        }

        return true;
    }

    public static void main(String[] args) {

        int[][] rooms1 = {{1}, {2}, {3}, {}};

        System.out.println(canVisitAllRooms(rooms1)); // true

        int[][] rooms2 = {{1}, {}, {3}, {}};

        System.out.println(canVisitAllRooms(rooms2)); // true
    }
}
