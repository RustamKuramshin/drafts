package com.rustam.dev.coroutines.introductionkotlincoroutines.ch02

import kotlin.coroutines.Continuation

fun processData(value: String, cb: (String) -> Unit) {
    cb(value)
}

fun main() {
    processData("test") { println(it) }
}