package ru.kuramshindev.ru.kuramshindev.coroutines.introductionkotlincoroutines.ch02

fun processData(value: String, cb: (String) -> Unit) {
    cb(value)
}

fun main() {
    processData("test") { println(it) }
}