package com.rustam.dev.coroutines

suspend fun main() {
    println(helloWorld())
}

suspend fun helloWorld(): String =
    "Hello ${world()}"


suspend fun world(): String =
    "World"
