package ru.kuramshindev.coroutines.introductionkotlincoroutines.ch01.async.p02;

import reactor.core.publisher.Mono;

public class TokenService {
    public static Mono<String> getToken() throws Exception {
        return Mono.just("123");
    }
}
