package com.rustam.dev;

import java.util.Arrays;

public class StringArchiver {

    // AAAABBRTTAWWE -> A4B2RT2AW2E
    private static char[] compressString(char[] chars) {

        int i = 0;
        int repCharIdx = -1;
        int repCharCounter = 1;

        while (true) {

            if (i == chars.length - 1) break;

            if (chars[i] == chars[i+1]) {
                repCharCounter++;
                if (repCharIdx == -1) {
                    repCharIdx = i;
                }
            } else {
                if (repCharIdx != -1) {
                    for(int j = i; j >= repCharIdx + 1; j--) {
                        chars[j] = ' ';
                    }
                    chars[repCharIdx + 1] = (char) ('0' + repCharCounter);
                    repCharIdx = -1;
                    repCharCounter = 1;
                }
            }

            i++;
        }

        return chars;
    }

    public static void main(String[] args) {
        char[] chars = {'A','A','A','A','B','B','R','T','T','A','W','W','E'};
        var result = compressString(chars);
        System.out.println(Arrays.toString(result));
    }
}
