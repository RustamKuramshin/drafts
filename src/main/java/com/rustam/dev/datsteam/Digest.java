package com.rustam.dev.datsteam;

import java.util.HashMap;

/**
 * Задача на code review.
 * Метод calc() считает криптографическую подпись для приходящих входных данных.
 * Попутно кэшируются результаты вычислений, чтобы не считать одно и тоже.
 * Метод doDigest() CPU-ёмкий, нагружает только CPU.
 * Хотелось бы, добиться максимальной пропускной способности метода calc().
 * Код будет выполняться в многопоточной среде.
 * Найти проблемы в коде и исправить.
 */
public abstract class Digest {

    public static HashMap<byte[], byte[]> cache = new HashMap<byte[], byte[]>();

    /**
     * Этот метод будет выполнять 1 тысяча потоков
     */
    public synchronized byte[] calc(byte[] input) {

        byte[] result = cache.get(input);

        if (result == null) {
            result = doDigest(input);
            cache.put(input, result);
        }

        return result;
    }

    protected abstract byte[] doDigest(byte[] input);
}
