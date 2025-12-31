package com.fuzis.search.repository;

import com.fuzis.search.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    // Метод для получения книг с изображениями (без жанров и авторов)
    @Query("SELECT b FROM Book b " +
            "LEFT JOIN FETCH b.photoLink pl " +
            "LEFT JOIN FETCH pl.imageData " +
            "ORDER BY b.bookId")
    Page<Book> findAllWithImages(Pageable pageable);

    // Метод для получения книг с жанрами (без авторов)
    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN FETCH b.genres " +
            "WHERE b.bookId IN :bookIds")
    List<Book> findBooksWithGenresByIds(@Param("bookIds") List<Integer> bookIds);

    // Метод для получения книг с авторами (без жанров)
    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN FETCH b.authors " +
            "WHERE b.bookId IN :bookIds")
    List<Book> findBooksWithAuthorsByIds(@Param("bookIds") List<Integer> bookIds);

    // Метод для получения книг с изображениями по ID
    @Query("SELECT b FROM Book b " +
            "LEFT JOIN FETCH b.photoLink pl " +
            "LEFT JOIN FETCH pl.imageData " +
            "WHERE b.bookId IN :bookIds")
    List<Book> findBooksWithImagesByIds(@Param("bookIds") List<Integer> bookIds);

    // Метод для получения всех ID книг (для пейджинга)
    @Query(value = "SELECT b.bookId FROM Book b ORDER BY b.bookId")
    Page<Integer> findAllBookIds(Pageable pageable);
}