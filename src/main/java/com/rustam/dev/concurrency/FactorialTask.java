package com.rustam.dev.concurrency;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class FactorialTask extends RecursiveTask<Long> {
    private final long n;

    public FactorialTask(long n) {
        this.n = n;
    }

    @Override
    protected Long compute() {
        if (n <= 1) {
            return n;
        }
        FactorialTask subTask = new FactorialTask(n - 1);
        subTask.fork(); // асинхронный запуск подзадачи
        return n * subTask.join(); // ожидание результата подзадачи и его использование
    }

    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();
        long number = 5;
        long result = pool.invoke(new FactorialTask(number));
        System.out.println("Factorial of " + number + " is: " + result);
    }
}