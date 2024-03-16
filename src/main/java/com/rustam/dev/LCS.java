package com.rustam.dev;

// Longest Common Subsequence (
public class LCS {

    public static String findLCS(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        // Создаем двумерный массив для хранения длин LCS для подстрок s1 и s2.
        // lcsLength[i][j] будет содержать длину LCS для s1[0..i-1] и s2[0..j-1].
        int[][] lcsLength = new int[m + 1][n + 1];

        // Заполняем массив lcsLength
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    lcsLength[i][j] = 0;
                } else if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    lcsLength[i][j] = lcsLength[i - 1][j - 1] + 1;
                } else {
                    lcsLength[i][j] = Math.max(lcsLength[i - 1][j], lcsLength[i][j - 1]);
                }
            }
        }

        // Восстанавливаем LCS из lcsLength
        StringBuilder lcs = new StringBuilder();
        int i = m, j = n;
        while (i > 0 && j > 0) {
            if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                lcs.insert(0, s1.charAt(i - 1));
                i--;
                j--;
            } else if (lcsLength[i - 1][j] > lcsLength[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        return lcs.toString();
    }

    public static void main(String[] args) {
        String s1 = "ABCBDAB";
        String s2 = "BDCAB";
        System.out.println("LCS: " + findLCS(s1, s2));  // Вывод: BCAB
    }
}

