package com.rustam.dev.introductionkotlincoroutines.ch05

import kotlinx.coroutines.*

suspend fun main() = coroutineScope {

    val threadLocal = ThreadLocal<String>()
    threadLocal.set("main")

    val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
        println("Начато в потоке: ${Thread.currentThread()}, thread local: '${threadLocal.get()}'")
        yield()
        println("Продолжено в потоке: ${Thread.currentThread()}, thread local: '${threadLocal.get()}'")
    }

}