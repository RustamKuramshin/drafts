package com.rustam.dev.introductionkotlincoroutines.ch01.suspend

suspend fun main() {
    asyncFun()
}

suspend fun asyncFun() {

    val res = otherAsyncFun()

    anotherAsyncFun(res)
}

suspend fun otherAsyncFun(): String {
    return "test"
}

suspend fun anotherAsyncFun(v: String) {

}