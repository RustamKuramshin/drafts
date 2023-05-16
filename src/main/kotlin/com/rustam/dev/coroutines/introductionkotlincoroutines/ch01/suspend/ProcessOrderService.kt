package com.rustam.dev.coroutines.introductionkotlincoroutines.ch01.suspend

class ProcessOrderService {

    suspend fun processOrder() {

        val token = getToken()

        if (token.isNotEmpty()) {

            val res = reserve("123")

            if (res) {
                save(token)
            } else {
                throw Exception("Не удалось зарезервировать товар")
            }
        } else {
            throw Exception("Не удалось получить токен для транзакции")
        }
    }

}