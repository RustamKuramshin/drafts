package ru.kuramshindev.spring.book.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import ru.kuramshindev.spring.book.config.BookCatalogProperties;

@Component
public class BookCatalogClient {

    private final RestClient restClient;

    public BookCatalogClient(RestClient.Builder restClientBuilder, BookCatalogProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    public BookMetadataResponse fetchMetadata(String isbn) {
        BookMetadataResponse response = restClient.get()
                .uri("/api/catalog/books/{isbn}", isbn)
                .retrieve()
                .body(BookMetadataResponse.class);

        if (response == null) {
            throw new IllegalStateException("Catalog service returned an empty response for isbn=" + isbn);
        }

        return response;
    }
}
