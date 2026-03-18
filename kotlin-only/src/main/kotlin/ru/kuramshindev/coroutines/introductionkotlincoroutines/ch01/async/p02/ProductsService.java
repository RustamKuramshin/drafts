package ru.kuramshindev.coroutines.introductionkotlincoroutines.ch01.async.p02;

import reactor.core.publisher.Mono;

public class ProductsService {
    public static Mono<Boolean> reserve(String productId) throws Exception {
        return Mono.just(true);
    }
}
