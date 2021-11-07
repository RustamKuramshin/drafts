package com.rustam.dev;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;

public class JMH {

    @Benchmark
    @Fork(value = 1, warmups = 2)
    @BenchmarkMode(Mode.AverageTime)
    public int measureName() {
        return sum(10000, 111111);
    }

    public static int sum(int a, int b) {
        return a + b;
    }
}
