package com.rustam.dev.leetcode.p641;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирования двусторонней круговой очереди")
public class MyCircularDequeTest {

    @Test
    @DisplayName("Базовый пример Example 1")
    public void test_base_example() {

        // arrange
        MyCircularDeque cd = new MyCircularDeque(3);

        // act

        // assert
        assertTrue(cd.insertLast(1));
        assertTrue(cd.insertLast(2));
        assertTrue(cd.insertFront(3));
        assertFalse(cd.insertFront(4));
        assertEquals(2, cd.getRear());
        assertTrue(cd.isFull());
        assertTrue(cd.deleteLast());
        assertTrue(cd.insertFront(4));
        assertEquals(4, cd.getFront());

        assertArrayEquals(List.of(4, 3, 1).toArray(), cd.toList().toArray());

        System.out.println(cd.toList());
    }

    @Test
    @DisplayName("test case 2")
    public void test_case_2() {
        // ["MyCircularDeque","insertFront","getFront","isEmpty","deleteFront","insertLast","getRear","insertLast","insertFront","deleteLast","insertLast","isEmpty"]
        // [[8],[5],[],[],[],[3],[],[7],[7],[],[4],[]]

        // arrange
        MyCircularDeque cd = new MyCircularDeque(3);

        // act

        // assert
        assertTrue(cd.insertFront(5));
        assertEquals(5, cd.getFront());
        assertFalse(cd.isEmpty());
        assertTrue(cd.deleteFront());
        assertTrue(cd.insertLast(3));
        assertEquals(3, cd.getRear());
        assertTrue(cd.insertLast(7));
        assertTrue(cd.insertFront(7));
        assertTrue(cd.deleteLast());
        assertTrue(cd.insertLast(4));
        assertFalse(cd.isEmpty());

        assertArrayEquals(List.of(7, 3, 4).toArray(), cd.toList().toArray());

        System.out.println(cd.toList());
    }

    @Test
    @DisplayName("test case 8")
    public void test_case_8() {
        // ["+MyCircularDeque","+insertFront","+deleteLast","+getRear","+getFront","+getFront","+deleteFront","+insertFront","+insertLast","+insertFront","+getFront","+insertFront"]
        // [[4],[9],[],[],[],[],[],[6],[5],[9],[],[6]]

        // arrange
        MyCircularDeque cd = new MyCircularDeque(4);

        // act

        // assert
        assertTrue(cd.insertFront(9));
        assertTrue(cd.deleteLast());
        assertEquals(-1, cd.getRear());
        assertEquals(-1, cd.getFront());
        assertEquals(-1, cd.getFront());
        assertFalse(cd.deleteFront());
        assertTrue(cd.insertFront(6));
        assertTrue(cd.insertLast(5));
        assertTrue(cd.insertFront(9));
        assertEquals(9, cd.getFront());
        assertTrue(cd.insertFront(6));

        assertArrayEquals(List.of(6, 9, 6, 5).toArray(), cd.toList().toArray());

        System.out.println(cd.toList());
    }
}
