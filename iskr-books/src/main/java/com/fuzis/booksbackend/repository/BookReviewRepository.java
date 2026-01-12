package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BookReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Integer> {

    @Query("SELECT br.book.bookId, AVG(br.score) as averageRating " +
            "FROM BookReview br " +
            "WHERE br.book.bookId IN :bookIds " +
            "GROUP BY br.book.bookId")
    List<Object[]> findAverageRatingsByBookIds(@Param("bookIds") List<Integer> bookIds);

    @Query("SELECT AVG(br.score) FROM BookReview br WHERE br.book.bookId = :bookId")
    Optional<Double> findAverageRatingByBookId(@Param("bookId") Integer bookId);

    List<BookReview> findByBook_BookIdIn(List<Integer> bookIds);

    @Query("SELECT br FROM BookReview br " +
            "LEFT JOIN FETCH br.user " +
            "WHERE br.book.bookId = :bookId")
    Page<BookReview> findByBook_BookId(@Param("bookId") Integer bookId, Pageable pageable);

    @Query("SELECT COUNT(br) FROM BookReview br WHERE br.book.bookId = :bookId")
    long countByBookId(@Param("bookId") Integer bookId);

    Optional<BookReview> findByUser_UserIdAndBook_BookId(Integer userId, Integer bookId);

    boolean existsByUser_UserIdAndBook_BookId(Integer userId, Integer bookId);
}