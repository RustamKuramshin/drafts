package com.rustam.dev.replit.stackimpl;

/* Интерфейс стека:
push(value) - кладет элемент на стека
pop() - снимает элемент со стека и возвращает его
peek() - возвращает элемент с вершины стека (без снятия)
isEmpty() - проверяет пуст ли стек
isFull() - проверяет полон ли стек
min() - возвращает минимальный элемент в стеке за O(n) или за O(1),
зависит от реализации
*/

public interface IStack {
    void push(int value) throws Exception;
    int pop() throws Exception;
    int peek();
    boolean isEmpty();
    boolean isFull();
    int min();
}
