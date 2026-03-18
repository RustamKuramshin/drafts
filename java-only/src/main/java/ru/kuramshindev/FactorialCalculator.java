package ru.kuramshindev;

import java.math.BigInteger;

public class FactorialCalculator {

    public BigInteger calculate(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Факториал не определен для отрицательных чисел: " + number);
        }

        BigInteger result = BigInteger.ONE;
        for (int current = 2; current <= number; current++) {
            result = result.multiply(BigInteger.valueOf(current));
        }
        return result;
    }
}
