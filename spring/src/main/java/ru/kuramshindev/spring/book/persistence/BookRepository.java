package ru.kuramshindev.spring.book.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.kuramshindev.spring.book.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
}
