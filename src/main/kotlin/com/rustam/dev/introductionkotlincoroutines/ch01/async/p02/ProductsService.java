package com.rustam.dev.introductionkotlincoroutines.ch01.async.p02;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public class ProductsService {
    public static Mono<Boolean> reserve(String productId) throws Exception {
        return Mono.just(true);
    }
}
