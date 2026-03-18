package ru.kuramshindev.spring.book.api;

import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotBlank String isbn
) {
}
