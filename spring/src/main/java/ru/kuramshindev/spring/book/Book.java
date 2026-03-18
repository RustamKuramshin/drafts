package ru.kuramshindev.spring.book;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String sourceSystem;

    @Column(nullable = false, length = 1024)
    private String summary;

    protected Book() {
    }

    public Book(String title, String author, String isbn, String sourceSystem, String summary) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.sourceSystem = sourceSystem;
        this.summary = summary;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public String getSummary() {
        return summary;
    }
}
