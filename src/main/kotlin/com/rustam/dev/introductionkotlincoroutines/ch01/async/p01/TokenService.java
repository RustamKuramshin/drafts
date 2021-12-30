package com.rustam.dev.introductionkotlincoroutines.ch01.async.p01;

import java.util.concurrent.CompletableFuture;

public class TokenService {
    public static CompletableFuture<String> getToken() throws Exception {
        return CompletableFuture.supplyAsync(() -> "123");
    }
}
