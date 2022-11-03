package com.rustam.dev.replit.stackimpl;

public class Stack implements IStack {

    private final int[][] stack;
    private int top;
    private final int capacity;

    public Stack(int capacity) {
        this.stack = new int[capacity][2];
        this.capacity = capacity;
        this.top = -1;
    }

    @Override
    public void push(int value) {
        if (isFull()) {
            throw new RuntimeException("Stack is full");
        }
        if (isEmpty()) {
            this.stack[++this.top] = new int[]{value, value};
        } else {
            if (value <= this.stack[this.top][1]) {
                this.stack[++this.top] = new int[]{value, value};
            } else {
                int nexTop = this.top + 1;
                this.stack[nexTop] = new int[]{value, this.stack[this.top][1]};
                this.top = nexTop;
            }
        }
    }

    @Override
    public int pop() {
        if (isEmpty()) {
            throw new RuntimeException("Stack is empty");
        }
        return this.stack[this.top--][0];
    }

    @Override
    public int peek() {
        if (isEmpty()) {
            throw new RuntimeException("Stack is empty");
        }
        return this.stack[this.top][0];
    }

    @Override
    public int min() {
        return this.stack[this.top][1];
    }

    @Override
    public boolean isEmpty() {
        return this.top == -1;
    }

    @Override
    public boolean isFull() {
        return this.top == this.capacity - 1;
    }
}
