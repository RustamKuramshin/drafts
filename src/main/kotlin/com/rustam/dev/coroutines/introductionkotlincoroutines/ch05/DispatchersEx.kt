package com.rustam.dev.coroutines.introductionkotlincoroutines.ch05

import kotlinx.coroutines.*

suspend fun main(): Unit = coroutineScope {

    launch {
        println("Унаследован родительский контекст : Я работаю в потоке ${Thread.currentThread().name}")
    }

    launch(Dispatchers.Unconfined) { // not confined -- will work with main thread
        println("Диспетчер Dispatchers.Unconfined  : Я работаю в потоке ${Thread.currentThread().name}")
    }

    launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher
        println("Диспетчер Dispatchers.Default     : Я работаю в потоке ${Thread.currentThread().name}")
    }

    launch(newSingleThreadContext("other-thread")) { // will get its own new thread
        println("Выполнение на отдельном потоке    : Я работаю в потоке ${Thread.currentThread().name}")
    }

    withContext(Dispatchers.IO) {
        Thread.sleep(3000)
        println("Выполнение на thread pool для IO : Я работаю в потоке ${Thread.currentThread().name}")
    }

}