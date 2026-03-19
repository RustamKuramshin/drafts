package ru.kuramshindev.replit.unpairedsearch;

import ru.kuramshindev.replit.unpairedsearch.Main;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Поиск элемента без пары")
class MainTest {

    @Test
    @DisplayName("Находит непарный элемент в середине массива")
    void testFindItemWithoutPair_1() {
        int[] arr1 = {2, 6, 3, 9, 2, 3, 6};

        assertEquals(9, ru.kuramshindev.replit.unpairedsearch.Main.findItemWithoutPair(arr1));
    }

    @Test
    @DisplayName("Находит непарный элемент в начале массива")
    void testFindItemWithoutPair_2() {
        int[] arr1 = {4, 1, 1, 2, 2, 3, 3};

        assertEquals(4, ru.kuramshindev.replit.unpairedsearch.Main.findItemWithoutPair(arr1));
    }

    @Test
    @DisplayName("Корректно обрабатывает массив из одного элемента")
    void testFindItemWithoutPair_3() {
        int[] arr1 = {10};

        assertEquals(10, ru.kuramshindev.replit.unpairedsearch.Main.findItemWithoutPair(arr1));
    }

    @Test
    @DisplayName("Корректно обрабатывает ноль как непарный элемент")
    void testFindItemWithoutPair_4() {
        int[] arr1 = {1, 0, 1};

        assertEquals(0, Main.findItemWithoutPair(arr1));
    }
}