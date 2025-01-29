package com.rustam.dev

import java.util.stream.Stream

fun main() {
    val list = listOf("qw", "qwe", "qwer", "qwerty")

    val even = list.stream().filterEvenLength().toList()

    println(even)
}

fun Stream<String>.filterEvenLength(): Stream<String> {
    return this.filter { it.length%2 == 0 }
}