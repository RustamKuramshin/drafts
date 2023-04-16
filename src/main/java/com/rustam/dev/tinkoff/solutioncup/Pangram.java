package com.rustam.dev.tinkoff.solutioncup;

import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Pangram {

    private static Map<Character, Integer> characterCount(String inputString) {

        Map<Character, Integer> charCountMap = new TreeMap<>();

        char[] strArray = inputString.toCharArray();

        for (char c : strArray) {

            if (!Character.isLetter(c)) continue;

            c = Character.toLowerCase(c);

            if (!((c >= 'а' && c <= 'я') || c == 'ё')) {
                continue;
            }

            if (charCountMap.containsKey(c)) {
                charCountMap.put(c, charCountMap.get(c) + 1);
            } else {
                charCountMap.put(c, 1);
            }
        }

        return charCountMap;
    }

    public static void main(String args[]) {

        var scanner = new Scanner(System.in);
        var input = "Съешь же ещё этих мягких фрнцузских булок, д выпей чю.";

        String alphabet = "абвгдежзийклмнопрстуфхцчшщъыьэюяё";

        Map<Character, Integer> charMap = characterCount(input);

        String res = charMap.keySet().stream().map(Object::toString).collect(Collectors.joining());

        if (res.equals(alphabet)) {
            System.out.println("True");
        } else {
            System.out.println("False");
        }
    }
}
