package com.rustam.dev.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class FJ extends RecursiveTask<Integer> {

    private Integer n;

    public FJ(Integer n) {
        this.n = n;
    }

    @Override
    protected Integer compute() {

        if (n <= 1) {
            return n;
        }

        FJ calc = new FJ(n - 1);

        calc.fork();

        return n * n + calc.join();
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        var p = new ForkJoinPool();

        FJ calculator = new FJ(400);

        p.execute(calculator);

        var r = calculator.get();

        System.out.println(r);
    }
}
