package com.rustam.dev.codeforces.p1788E;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

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

        int[] arr = new int[n];
        int arrSum = 0;
        boolean allNeg = true;

        for (int i = 0; i < n; i++) {
            int num = Reader.nextInt();
            arr[i] = num;
            arrSum += num;
            if (num > 0) {
                allNeg = false;
            }
        }

        out.println(getMaxSumSegmentLengths(arr, arrSum, allNeg));

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
