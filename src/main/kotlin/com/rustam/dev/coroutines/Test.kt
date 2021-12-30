package com.rustam.dev.coroutines

import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

suspend fun main() {
    println(suspendFunction())
}

suspend fun suspendFunction(): String {
    return processMsg("test_value")
}

suspend fun processMsg(msg: String): String {
    return msg
}

interface ContinuationInterceptor : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<ContinuationInterceptor>
    fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
    fun releaseInterceptedContinuation(continuation: Continuation<*>)
}