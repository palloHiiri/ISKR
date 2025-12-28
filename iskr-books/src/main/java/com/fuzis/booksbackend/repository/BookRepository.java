package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH b.genres " +
            "WHERE b.bookId = :id")
    Optional<Book> findByIdWithAuthorsAndGenres(@Param("id") Integer id);

    @Query("SELECT b FROM Book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH b.genres")
    Page<Book> findAllWithAuthorsAndGenres(Pageable pageable);

    boolean existsByIsbn(String isbn);
}