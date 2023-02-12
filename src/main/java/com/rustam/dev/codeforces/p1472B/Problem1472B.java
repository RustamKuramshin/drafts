package com.rustam.dev.codeforces.p1472B;

import java.io.*;
import java.util.StringTokenizer;

// https://codeforces.com/problemset/problem/1472/B
public class Problem1472B {

    public static PrintWriter out;

    public static class Reader {

        static BufferedReader reader;
        static StringTokenizer tokenizer;

        /** call this method to initialize reader for InputStream */
        static void init(InputStream input) {
            reader = new BufferedReader(new InputStreamReader(input));
            tokenizer = new StringTokenizer("");
        }

        /** get next word */
        static String next() throws IOException {
            while ( ! tokenizer.hasMoreTokens() ) {
                //TODO add check for eof if necessary
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

        int datasetsCount = Reader.nextInt();

        for (int i = 0; i < datasetsCount; i++) {

            int candiesCount = Reader.nextInt();

            int c1 = 0, c2 = 0;

            for(int j = 0; j < candiesCount; j++) {

                int candy = Reader.nextInt();

                if (candy == 1) {
                    ++c1;
                } else if (candy == 2) {
                    ++c2;
                }
            }

            if (c1 == 0) {
                if (c2%2 == 0) {
                    out.println("YES");
                    continue;
                }
                out.println("NO");
                continue;
            }

            if (c1%2 == 0) {
                out.println("YES");
            } else {
                out.println("NO");
            }
        }

        out.close();
    }
}

