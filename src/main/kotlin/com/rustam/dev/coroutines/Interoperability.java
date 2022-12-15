package com.rustam.dev.coroutines;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;

public class Interoperability {
    public static void main(String[] args) {
        Continuation<String> continuation = new Continuation<String>() {
            @Override
            public void resumeWith(@NotNull Object o) {
            }
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }
        };

        var res = Coroutines3Kt.helloWorld(continuation);
    }
}
