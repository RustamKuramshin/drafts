package com.rustam.dev.coroutines;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.jetbrains.annotations.NotNull;

public class Test {
    public static void main(String[] args) {

        var o = new Continuation<String>() {

            @Override
            public void resumeWith(@NotNull Object o) {
                System.out.println("resumeWith");
            }

            @Override
            public CoroutineContext getContext() {
                System.out.println("getContext");
                return EmptyCoroutineContext.INSTANCE;
            }
        };

        
        var i = HelloWorldKt.testSusp(o);
        System.out.println(i);
    }
}
