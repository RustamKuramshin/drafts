package ru.kuramshindev;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 1, time = 5)
@Fork(1)
public class JMH {

    @Param({"1", "10", "100", "1000", "10000"})
    public int howMany;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(JMH.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public List<String> testArray() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < howMany; ++i) {
            list.add("" + i);
        }
        return list;
    }

    @Benchmark
    public List<String> testLinked() {
        List<String> list = new LinkedList<>();
        for (int i = 0; i < howMany; ++i) {
            list.add("" + i);
        }
        return list;
    }

}
