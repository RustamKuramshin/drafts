package com.rustam.dev.introductionkotlincoroutines.ch05

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun main() = coroutineScope{

    val job = launch {
        repeat(1000) { i ->
            println("Спасть: $i ...")
            delay(100L)
        }
    }

    delay(1000L)
    job.cancel()
    job.join()

}