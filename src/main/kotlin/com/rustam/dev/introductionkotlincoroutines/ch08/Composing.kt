package com.rustam.dev.introductionkotlincoroutines.ch08

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.system.measureTimeMillis

suspend fun doSomethingOne(): Int {
    delay(1000L)
    return 13
}

suspend fun doSomethingTwo(): Int {
    delay(1000L)
    return 29
}

suspend fun concurrentSum(): Int = coroutineScope {
    val one = async { doSomethingOne() }
    val two = async { doSomethingTwo() }
    one.await() + two.await()
}


suspend fun main() = coroutineScope {

    val time = measureTimeMillis {
        val one = async { doSomethingOne() }
        val two = async { doSomethingTwo() }
    }

    println("Выполнено за $time ms")

}
