package com.rustam.dev.codeforces.p1472B;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class ReadInputWriteOutputExample {

    public static PrintWriter out;

    // https://codeforces.com/blog/entry/7018?locale=ru
    public static class Reader {

        // Класс Reader может быть расширен любыми статическими методами парсинга строки

        static BufferedReader reader;
        static StringTokenizer tokenizer;

        static void init(InputStream input) {
            reader = new BufferedReader(new InputStreamReader(input));
            tokenizer = new StringTokenizer("");
        }

        static String next() throws IOException {
            while ( ! tokenizer.hasMoreTokens() ) {
                tokenizer = new StringTokenizer(reader.readLine());
            }
            return tokenizer.nextToken();
        }

        static int nextInt() throws IOException {
            return Integer.parseInt(next());
        }

        static double nextDouble() throws IOException {
            return Double.parseDouble(next());
        }
    }

    public static void main(String[] args) throws IOException {
        Reader.init(System.in);
        out = new PrintWriter(new BufferedOutputStream(System.out));

        // Далее идет ваш код

        // Пример чтения строк, целых и вещественных чисел
        Reader.next();
        Reader.nextInt();
        Reader.nextDouble();

        // Пишем в ответ
        out.println("Test");


        // Отправить накопленный вывод на stdout
        out.close();
    }
}
