package ru.kuramshindev.spring.book.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "book.catalog")
public record BookCatalogProperties(String baseUrl) {
}
