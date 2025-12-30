package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BookReview;
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

    Optional<Double> findAverageRatingByBook_BookId(Integer bookId);

    List<BookReview> findByBook_BookIdIn(List<Integer> bookIds);
}