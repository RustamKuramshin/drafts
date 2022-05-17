package com.rustam.dev.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking { // this: CoroutineScope
    launch { prt() }
    println("Hello") // main coroutine continues while a previous one is delayed
}

private suspend fun prt() {
    delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
    println("World!")
}