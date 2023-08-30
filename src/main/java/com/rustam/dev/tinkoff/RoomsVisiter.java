package com.rustam.dev.tinkoff;

import java.util.Stack;

public class RoomsVisiter {

    private static boolean canVisitAllRooms(int[][] rooms) {
        if (rooms == null || rooms.length == 0) return false;

        int n = rooms.length;
        boolean[] visited = new boolean[n];  // массив для отслеживания посещенных комнат
        Stack<Integer> stack = new Stack<>();  // стек для ключей/дверей

        stack.push(0);  // начинаем с двери 0

        while (!stack.isEmpty()) {
            int currentRoom = stack.pop();
            visited[currentRoom] = true;
            for (int key : rooms[currentRoom]) {
                if (!visited[key]) {
                    stack.push(key);
                }
            }
        }

        // Проверка, были ли все комнаты посещены
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
