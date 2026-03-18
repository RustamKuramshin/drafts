package ru.kuramshindev.ru.kuramshindev.coroutines.introductionkotlincoroutines

import kotlinx.coroutines.*

suspend fun main() = coroutineScope {
    repeat(100_000) {
        launch {
            delay(2000L)
            print("🌲")
        }
    }
}