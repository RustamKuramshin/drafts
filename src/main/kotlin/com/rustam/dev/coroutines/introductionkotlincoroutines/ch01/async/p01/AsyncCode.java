package com.rustam.dev.coroutines.introductionkotlincoroutines.ch01.async.p01;

import java.util.concurrent.CompletableFuture;

public class AsyncCode {

    public void processOrder() throws Exception {
        CompletableFuture<String> future = TokenService.getToken().thenCompose(token -> {
            try {
                ProductsService.reserve("123").thenAccept(res -> {
                    if (res) {
                        ClientDataRepository.save(token);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
        future.get();
    }

}
