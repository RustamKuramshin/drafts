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

        int x = 1;
        int y = x;

        int segmentSum = 0;
        int totalLengthAllSegments = 0;
        boolean allNeg = true;

        while (y <= n) {
            int a = Reader.nextInt();

            if (a > 0) allNeg = false;

            segmentSum += a;

            if (segmentSum < 0 && !allNeg) {
                totalLengthAllSegments += (y - 1) - x + 1;
                x = y;
            }

            y++;
        }

        out.println(totalLengthAllSegments);

        out.close();
    }

    private  static int getMaxSumSegmentLengths(int[] arr, int arrSum, boolean allNeg) {

        if (allNeg) return 0;

        int sum = 0;
        int sumLen = 0;
        int start = 1;

        for (int end = start + 1; end <= arr.length;) {

            sum += arr[start] + arr[end - 1];

            if (sum < 0) {
                sumLen += (end - 1) - start + 1;
                start = end;
                end = end + 1;
            }
        }

        return sumLen;
    }
}
