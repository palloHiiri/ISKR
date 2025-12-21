package com.fuzis.search.repository;

import com.fuzis.search.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query(value = "SELECT b FROM Book b ORDER BY b.bookId",
            countQuery = "SELECT COUNT(b) FROM Book b")
    Page<Book> findAllOrderedById(Pageable pageable);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.genres LEFT JOIN FETCH b.authors WHERE b.bookId IN :bookIds ORDER BY b.bookId")
    List<Book> findAllWithGenresByIds(@Param("bookIds") List<Integer> bookIds);

    @Query(value = "SELECT * FROM books.books ORDER BY book_id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Book> findAllNativeWithPagination(@Param("offset") int offset, @Param("limit") int limit);
}