package com.rustam.dev;

/**
 * Дан двусторонний массив (матрица) N x M.
 * Напишите функцию, которая поворачивает этот массив на 90 градусов по часовой стрелке.
 */
public class RotateMatrix {

    public int[][] rotateMatrix(int[][] matrix) {
        int n = matrix.length;
        if (n == 0 || n != matrix[0].length) return null;

        int[][] rotated = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rotated[j][n - 1 - i] = matrix[i][j];
            }
        }
        return rotated;
    }

}
