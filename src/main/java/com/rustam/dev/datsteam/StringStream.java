package com.rustam.dev.datsteam;

import java.util.Optional;
import java.util.stream.Stream;

public class StringStream {

    public static void main(String[] args) {
        // Какие оптимизации будут применены к этому стриму?

        // Что произойдет, если в стриме станет 1 миллион строк? Как оптимизировать?

        // Что произойдет в случае перехода на бесконечный генератор
        // строк типа Stream.iterate(1, n -> n + 1).map(String::valueOf) ?
        // Как findFirst() адоптирован для этого?

        Optional<String> op =  Stream.of("assddf", "dfdf", "ss", "gfdgf", "ss", "asssgfgf", "df")
                .filter(s -> s.contains("ss"))
                .filter(s -> s.length() > 2)
                .findFirst();

        op.ifPresent(System.out::println);
    }
}
