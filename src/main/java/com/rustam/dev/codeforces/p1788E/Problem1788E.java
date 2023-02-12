package com.rustam.dev.codeforces.p1788E;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

// https://codeforces.com/contest/1788/problem/E
public class Problem1788E {

    public static PrintWriter out;

    public static class Reader {

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

        static long nextLong() throws IOException {
            return Long.parseLong(next());
        }

        static double nextDouble() throws IOException {
            return Double.parseDouble(next());
        }

        static String nextLine() {
            String str = "";
            try {
                str = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return str;
        }
    }

    public static void main(String[] args) throws IOException {
        Reader.init(System.in);
        out = new PrintWriter(new BufferedOutputStream(System.out));

        int n = Reader.nextInt();

        int i = 1;

        int segmentSum = 0;
        int segmentLength = 0;
        int totalLengthAllSegments = 0;
        int totalSum = 0;
        boolean allNeg = true;

        while (i <= n) {
            int a = Reader.nextInt();

            if (a > 0) allNeg = false;

            segmentSum += a;
            if (segmentSum >= 0) {
                ++segmentLength;
            }

            totalSum += a;

            if (segmentSum < 0 && !allNeg) {
                totalLengthAllSegments += segmentLength;
                segmentSum = 0;
                segmentLength = 0;
            }

            if (i == n && segmentSum > 0) {
                totalLengthAllSegments += segmentLength;
                segmentSum = 0;
                segmentLength = 0;
            }

            if (i == n) {
                if (totalSum >= 0) {
                    totalLengthAllSegments = n;
                }
                if (totalSum < 0) {
                    if (totalSum - a >= 0) {
                        totalLengthAllSegments = n - 1;
                    }
                }
            }

            i++;
        }

        out.println(totalLengthAllSegments);

        out.close();
    }
}
