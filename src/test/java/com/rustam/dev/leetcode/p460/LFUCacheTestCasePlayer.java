package com.rustam.dev.leetcode.p460;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LFUCacheTestCasePlayer {

    private static String readFileToString(String filePath) {

        ClassLoader classLoader = LFUCacheTestCasePlayer.class.getClassLoader();
        StringBuilder sb = new StringBuilder();

        try (InputStream inputStream = classLoader.getResourceAsStream(filePath);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(ls);
            }

            sb.deleteCharAt(sb.length() - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static void playTestCase(String methodsFilePath, String testDataFilePath) {

        String methods = readFileToString(methodsFilePath);
        String testData = readFileToString(testDataFilePath);

        // processing methods
        List<String> methodsList = Arrays.stream(methods.substring(1, methods.length() - 1).split(","))
                .map(m -> m.replaceAll("\\\"", ""))
                .collect(Collectors.toList());

        // processing test data
        List<Integer[]> tdl = Arrays.stream(testData.substring(1, testData.length() - 1).split("],\\["))
                .map(td -> {

                    String tdWithoutBrackets = td.replace("[", "").replace("]", "");

                    if (tdWithoutBrackets.matches("(\\d+),(\\d+)")) {
                        var tsa = tdWithoutBrackets.split(",");
                        return new Integer[] {Integer.parseInt(tsa[0]), Integer.parseInt(tsa[1])};
                    } else {
                        return new Integer[]{Integer.parseInt(tdWithoutBrackets)};
                    }

                })
                .collect(Collectors.toList());

        if (methodsList.size() != tdl.size()) {
            throw new IllegalStateException("Кол-во методов и данных должно совпадать");
        }

        // play
        LFUCache lfu = null;

        for (int i = 0; i < methodsList.size() - 1; i++) {

            try {
                switch (methodsList.get(i)) {
                    case "LFUCache":
                        lfu = new LFUCache(tdl.get(i)[0]);
                        break;
                    case "put":
                        lfu.put(tdl.get(i)[0], tdl.get(i)[1]);
                        break;
                    case "get":
                        lfu.get(tdl.get(i)[0]);
                        break;
                    default:
                        throw new RuntimeException("Неизвестный метод");
                }
            } catch (Exception e) {
                System.out.println("Тестируемый код упал:");
                System.out.println(String.format("method = %s", methodsList.get(i)));
                System.out.println(String.format("test data = %s", Arrays.toString(tdl.get(i))));
                throw  new RuntimeException("Testing code call failed", e);
            }
        }
    }
}
