package com.rustam.dev.coroutines

import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun t3() {
    testSusp()
}

suspend fun testSusp(): String {
    return pr("tst666")
}

suspend fun pr(msg: String): String {
    return msg
}
