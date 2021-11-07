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

        var r = new ForkJoinPool().invoke(new FJ(4));

        System.out.println(r);

        System.out.println(Runtime.getRuntime().availableProcessors());
    }
}
