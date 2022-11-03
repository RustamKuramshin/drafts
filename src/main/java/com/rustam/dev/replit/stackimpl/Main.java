package com.rustam.dev.replit.stackimpl;

import org.junit.*;
import org.junit.runner.*;

import static org.junit.Assert.*;

// Требуется написать реализацию стека в файле Stack.java
public class Main {

    public static void main(String[] args) {
        System.out.println("Run stack test");
        JUnitCore.main(Main.class.getCanonicalName());
    }

    @Test
    public void testStack_1() throws Exception {
        Stack stackInst = new Stack(5);

        // Наполняем стек
        stackInst.push(1);
        stackInst.push(2);
        stackInst.push(3);
        stackInst.push(4);
        stackInst.push(5);

        // Проверим, что стек полон
        assertTrue(stackInst.isFull());

        // Прочитаем элемент с вершины стека
        assertEquals(5, stackInst.peek());

        // Снимем элементы со стека
        assertEquals(5, stackInst.pop());
        assertEquals(4, stackInst.pop());
        assertEquals(3, stackInst.pop());
        assertEquals(2, stackInst.pop());
        assertEquals(1, stackInst.pop());

        // Проверим, что стек пуст
        assertTrue(stackInst.isEmpty());
    }

    // Снятие с пустого стека
    @Test(expected = Exception.class)
    public void testStack_2() throws Exception {
        Stack stackInst = new Stack(5);
        stackInst.pop();
    }

    // Получение с пустого стека
    @Test(expected = Exception.class)
    public void testStack_3() throws Exception {
        Stack stackInst = new Stack(5);
        stackInst.peek();
    }

    // Переполнение стека
    @Test(expected = Exception.class)
    public void testStack_4() throws Exception {
        Stack stackInst = new Stack(5);

        stackInst.push(1);
        stackInst.push(2);
        stackInst.push(3);
        stackInst.push(4);
        stackInst.push(5);
        stackInst.push(6);
    }

    @Test
    public void testStack_5() throws Exception {
        Stack stackInst = new Stack(9);

        // Наполняем стек
        stackInst.push(9);
        stackInst.push(9);
        stackInst.push(3);
        stackInst.push(34);
        stackInst.push(17);
        stackInst.push(2);
        stackInst.push(26);
        stackInst.push(4);
        stackInst.push(1);
        assertEquals(1, stackInst.min());

        stackInst.pop();
        assertEquals(2, stackInst.min());

        stackInst.pop();
        stackInst.pop();
        stackInst.pop();
        assertEquals(3, stackInst.min());

        stackInst.pop();
        stackInst.pop();
        stackInst.pop();
        assertEquals(9, stackInst.min());

        stackInst.pop();
        assertEquals(9, stackInst.min());
    }
}