package com.rustam.dev;

import java.util.Arrays;

public class NQueens {
    // Размер доски и массив для хранения позиции ферзей
    private int[] board;
    private int N;

    public NQueens(int N) {
        this.N = N;
        this.board = new int[N];
        Arrays.fill(board, -1);
    }

    // Проверка, безопасно ли разместить ферзя
    private boolean isSafe(int row, int col) {
        for (int i = 0; i < row; i++) {
            if (board[i] == col || Math.abs(row - i) == Math.abs(col - board[i])) {
                return false;
            }
        }
        return true;
    }

    // Функция для решения задачи о N ферзях
    public boolean solveNQueens(int row) {
        if (row == N) {
            return true; // Все ферзи успешно размещены
        }
        for (int col = 0; col < N; col++) {
            if (isSafe(row, col)) {
                board[row] = col;
                if (solveNQueens(row + 1)) {
                    return true;
                }
                board[row] = -1; // Откат, если не нашли решение
            }
        }
        return false;
    }

    // Вывод решения
    public void printSolution() {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(board[i] == j ? "Q " : ". ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        NQueens nQueens = new NQueens(4); // Пример для 4x4 доски
        if (nQueens.solveNQueens(0)) {
            nQueens.printSolution();
        } else {
            System.out.println("Решений не найдено");
        }
    }
}

