package ru.kuramshindev.spring.book.client;

public record BookMetadataResponse(
        String sourceSystem,
        String summary
) {
}
