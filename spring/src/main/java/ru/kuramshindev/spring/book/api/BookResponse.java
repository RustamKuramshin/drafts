package ru.kuramshindev.spring.book.api;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String sourceSystem,
        String summary
) {
}
