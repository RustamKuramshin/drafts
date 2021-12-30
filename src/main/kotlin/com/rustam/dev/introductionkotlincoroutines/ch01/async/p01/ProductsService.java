package com.rustam.dev.introductionkotlincoroutines.ch01.async.p01;

import java.util.concurrent.CompletableFuture;

public class ProductsService {
    public static CompletableFuture<Boolean> reserve(String productId) throws Exception {
        return CompletableFuture.supplyAsync(() -> true);
    }
}
