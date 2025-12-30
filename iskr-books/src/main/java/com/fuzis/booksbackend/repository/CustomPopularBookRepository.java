package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomPopularBookRepository extends JpaRepository<Book, Integer> {

    @Query(value = """
        SELECT b.* FROM BOOKS.BOOKS b
        LEFT JOIN BOOKS.BOOKS_BOOK_COLLECTIONS bbc ON b.book_id = bbc.book_id
        GROUP BY b.book_id
        ORDER BY COUNT(bbc.c_book_bcol_id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Book> findTopPopularBooks(@Param("limit") int limit);
}