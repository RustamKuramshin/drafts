package com.rustam.dev;

import java.util.function.Consumer;

public class Lambda {
    public static void main(String[] args) {
        Consumer<String> greeter = (String name) -> System.out.printf("Hello, %s\n", name);
        greeter.accept("John");
    }
}

