package ru.kuramshindev.spring.book;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.kuramshindev.spring.book.api.BookResponse;
import ru.kuramshindev.spring.book.api.CreateBookRequest;
import ru.kuramshindev.spring.book.client.BookCatalogClient;
import ru.kuramshindev.spring.book.client.BookMetadataResponse;
import ru.kuramshindev.spring.book.persistence.BookRepository;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final BookCatalogClient bookCatalogClient;

    public BookService(BookRepository bookRepository, BookCatalogClient bookCatalogClient) {
        this.bookRepository = bookRepository;
        this.bookCatalogClient = bookCatalogClient;
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public BookResponse findById(Long id) {
        return bookRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        BookMetadataResponse metadata = bookCatalogClient.fetchMetadata(request.isbn());
        Book book = new Book(
                request.title(),
                request.author(),
                request.isbn(),
                metadata.sourceSystem(),
                metadata.summary()
        );

        return toResponse(bookRepository.save(book));
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getSourceSystem(),
                book.getSummary()
        );
    }
}
