package com.rustam.dev.leetcode.misc.p232;

import java.util.ArrayDeque;
import java.util.Deque;

// https://leetcode.com/problems/implement-queue-using-stacks/
public class MyQueue {

    private final Deque<Integer> inputStack;

    private final Deque<Integer> outputStack;

    private void transferItems() {
        while (!inputStack.isEmpty()) {
            outputStack.push(inputStack.peek());
        }
    }

    private boolean isOneItemInInput() {
        return inputStack.size() == 1 && outputStack.isEmpty();
    }

    public MyQueue() {
        inputStack = new ArrayDeque<>();
        outputStack = new ArrayDeque<>();
    }

    public void push(int x) {
        inputStack.push(x);
    }

    public int pop() {
        if (isOneItemInInput()) {
            return inputStack.pop();
        }

        transferItems();

        return outputStack.pop();
    }

    public int peek() {
        if (isOneItemInInput()) {
            return inputStack.peek();
        }

        transferItems();

        return outputStack.peek();
    }

    public boolean empty() {
        return inputStack.isEmpty() && outputStack.isEmpty();
    }
}
