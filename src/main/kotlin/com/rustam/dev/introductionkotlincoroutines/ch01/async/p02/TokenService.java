package com.rustam.dev.introductionkotlincoroutines.ch01.async.p02;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public class TokenService {
    public static Mono<String> getToken() throws Exception {
        return Mono.just("123");
    }
}
