package com.rustam.dev.coroutines.introductionkotlincoroutines.ch05

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

suspend fun main() = coroutineScope {

    withTimeout(100L) {
        repeat(500) { i ->
            println("Спать $i ...")
            delay(100L)
        }
    }

}