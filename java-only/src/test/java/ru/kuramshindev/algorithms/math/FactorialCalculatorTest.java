package ru.kuramshindev.algorithms.math;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.kuramshindev.FactorialCalculator;

class FactorialCalculatorTest {

    private final FactorialCalculator factorialCalculator = new FactorialCalculator();

    @ParameterizedTest(name = "{0}! should equal {1}")
    @MethodSource("factorialArguments")
    @DisplayName("calculate should return the expected factorial value")
    void calculate_returnsExpectedFactorial(int input, String expected) {
        assertThat(factorialCalculator.calculate(input)).isEqualTo(new BigInteger(expected));
    }

    @Test
    @DisplayName("calculate should reject negative numbers")
    void calculate_throwsExceptionForNegativeNumbers() {
        assertThatThrownBy(() -> factorialCalculator.calculate(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("отрицательных чисел");
    }

    private static Stream<Arguments> factorialArguments() {
        return Stream.of(
                Arguments.of(0, "1"),
                Arguments.of(1, "1"),
                Arguments.of(5, "120"),
                Arguments.of(10, "3628800"),
                Arguments.of(20, "2432902008176640000")
        );
    }
}
