package com.fuzis.search.repository;

import com.fuzis.search.entity.BookReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Integer> {

    @Query("SELECT br.book.bookId, AVG(br.score) as averageRating " +
            "FROM BookReview br " +
            "WHERE br.book.bookId IN :bookIds " +
            "GROUP BY br.book.bookId")
    List<Object[]> findAverageRatingsByBookIds(@Param("bookIds") List<Integer> bookIds);
}
