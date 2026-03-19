package ru.kuramshindev.replit.stack;

import ru.kuramshindev.replit.stack.Stack;
import ru.kuramshindev.replit.stack.StackImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Проверка реализации стека")
class StackTest {

    @Test
    @DisplayName("Стек корректно работает при заполнении и извлечении")
    void testStack_1() throws Exception {
        ru.kuramshindev.replit.stack.Stack stackImplInst = new ru.kuramshindev.replit.stack.StackImpl(5);

        stackImplInst.push(1);
        stackImplInst.push(2);
        stackImplInst.push(3);
        stackImplInst.push(4);
        stackImplInst.push(5);

        assertTrue(stackImplInst.isFull());
        assertEquals(5, stackImplInst.peek());
        assertEquals(5, stackImplInst.pop());
        assertEquals(4, stackImplInst.pop());
        assertEquals(3, stackImplInst.pop());
        assertEquals(2, stackImplInst.pop());
        assertEquals(1, stackImplInst.pop());
        assertTrue(stackImplInst.isEmpty());
    }

    @Test
    @DisplayName("Снятие с пустого стека выбрасывает исключение")
    void testStack_2() {
        ru.kuramshindev.replit.stack.Stack stackImplInst = new ru.kuramshindev.replit.stack.StackImpl(5);

        assertThrows(RuntimeException.class, stackImplInst::pop);
    }

    @Test
    @DisplayName("Получение вершины пустого стека выбрасывает исключение")
    void testStack_3() {
        ru.kuramshindev.replit.stack.Stack stackImplInst = new ru.kuramshindev.replit.stack.StackImpl(5);

        assertThrows(RuntimeException.class, stackImplInst::peek);
    }

    @Test
    @DisplayName("Переполнение стека выбрасывает исключение")
    void testStack_4() throws Exception {
        ru.kuramshindev.replit.stack.Stack stackImplInst = new ru.kuramshindev.replit.stack.StackImpl(5);

        stackImplInst.push(1);
        stackImplInst.push(2);
        stackImplInst.push(3);
        stackImplInst.push(4);
        stackImplInst.push(5);

        assertThrows(RuntimeException.class, () -> stackImplInst.push(6));
    }

    @Test
    @DisplayName("Минимум в стеке пересчитывается при удалении элементов")
    void testStack_5() throws Exception {
        Stack stackImplInst = new StackImpl(9);

        stackImplInst.push(9);
        stackImplInst.push(9);
        stackImplInst.push(3);
        stackImplInst.push(34);
        stackImplInst.push(17);
        stackImplInst.push(2);
        stackImplInst.push(26);
        stackImplInst.push(4);
        stackImplInst.push(1);
        assertEquals(1, stackImplInst.min());

        stackImplInst.pop();
        assertEquals(2, stackImplInst.min());

        stackImplInst.pop();
        stackImplInst.pop();
        stackImplInst.pop();
        assertEquals(3, stackImplInst.min());

        stackImplInst.pop();
        stackImplInst.pop();
        stackImplInst.pop();
        assertEquals(9, stackImplInst.min());

        stackImplInst.pop();
        assertEquals(9, stackImplInst.min());
    }
}