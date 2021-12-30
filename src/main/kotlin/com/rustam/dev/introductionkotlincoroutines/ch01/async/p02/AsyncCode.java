package com.rustam.dev.introductionkotlincoroutines.ch01.async.p02;

import reactor.core.publisher.Mono;

public class AsyncCode {

    private static String t = null;

    public void processOrder() throws Exception {
        TokenService.getToken().flatMap(token -> {
            t = token;
            Mono<Boolean> r = null;
            try {
                r = ProductsService.reserve("123");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return r;
        }).mapNotNull(res -> {
            if (res) {
                ClientDataRepository.save(t);
            }
            return null;
        }).subscribe();
    }

}
