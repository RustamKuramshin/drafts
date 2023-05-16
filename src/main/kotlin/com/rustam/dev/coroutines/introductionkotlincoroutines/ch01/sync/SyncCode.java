package com.rustam.dev.coroutines.introductionkotlincoroutines.ch01.sync;

public class SyncCode {

    public void processOrder() throws Exception {

        String token = TokenService.getToken();

        if (token.length() != 0) {

            Boolean res = ProductsService.reserve("123");

            if (res) {
                ClientDataRepository.save(token);
            } else {
                throw new Exception("Не удалось зарезервировать товар");
            }
        } else {
            throw new Exception("Не удалось получить токен для транзакции");
        }
    }
}
