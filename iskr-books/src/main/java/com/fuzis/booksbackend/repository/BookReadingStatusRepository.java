package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BookReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookReadingStatusRepository extends JpaRepository<BookReadingStatus, Integer> {

    Optional<BookReadingStatus> findByUserIdAndBook_BookId(Integer userId, Integer bookId);

    List<BookReadingStatus> findByUserId(Integer userId);

    @Query("SELECT brs FROM BookReadingStatus brs WHERE brs.userId = :userId AND brs.readingStatus = :readingStatus")
    List<BookReadingStatus> findByUserIdAndReadingStatus(@Param("userId") Integer userId,
                                                         @Param("readingStatus") String readingStatus);

    @Query("SELECT COUNT(brs) FROM BookReadingStatus brs WHERE brs.userId = :userId AND brs.readingStatus = :readingStatus")
    Long countByUserIdAndReadingStatus(@Param("userId") Integer userId,
                                       @Param("readingStatus") String readingStatus);

    @Query("SELECT COALESCE(SUM(brs.pageRead), 0) FROM BookReadingStatus brs WHERE brs.userId = :userId")
    Integer sumPagesReadByUserId(@Param("userId") Integer userId);

    @Query("SELECT COUNT(brs) FROM BookReadingStatus brs WHERE brs.userId = :userId AND brs.readingStatus = 'Finished'")
    Long countFinishedBooksByUserId(@Param("userId") Integer userId);

    boolean existsByUserIdAndBook_BookId(Integer userId, Integer bookId);
}