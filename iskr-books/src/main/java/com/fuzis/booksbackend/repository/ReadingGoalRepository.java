package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.ReadingGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Integer> {

    List<ReadingGoal> findByUserId(Integer userId);

    Optional<ReadingGoal> findByPgoalIdAndUserId(Integer pgoalId, Integer userId);

    @Query(value = "SELECT * FROM BOOKS.READING_GOALS rg WHERE rg.user_id = :userId " +
            "AND rg.start_date <= :currentDate " +
            "AND (rg.start_date + " +
            "  CASE rg.period " +
            "    WHEN '1d' THEN INTERVAL '1 day' " +
            "    WHEN '3d' THEN INTERVAL '3 days' " +
            "    WHEN 'week' THEN INTERVAL '1 week' " +
            "    WHEN 'month' THEN INTERVAL '1 month' " +
            "    WHEN 'quarter' THEN INTERVAL '3 months' " +
            "    WHEN 'year' THEN INTERVAL '1 year' " +
            "  END) >= :currentDate", nativeQuery = true)
    List<ReadingGoal> findActiveGoalsByUserId(@Param("userId") Integer userId,
                                              @Param("currentDate") LocalDateTime currentDate);

    @Query(value = "SELECT COUNT(*) FROM BOOKS.READING_GOALS rg WHERE rg.user_id = :userId " +
            "AND (rg.start_date + " +
            "  CASE rg.period " +
            "    WHEN '1d' THEN INTERVAL '1 day' " +
            "    WHEN '3d' THEN INTERVAL '3 days' " +
            "    WHEN 'week' THEN INTERVAL '1 week' " +
            "    WHEN 'month' THEN INTERVAL '1 month' " +
            "    WHEN 'quarter' THEN INTERVAL '3 months' " +
            "    WHEN 'year' THEN INTERVAL '1 year' " +
            "  END) >= :currentDate", nativeQuery = true)
    Long countActiveGoalsByUserId(@Param("userId") Integer userId,
                                  @Param("currentDate") LocalDateTime currentDate);

    @Query(value = "SELECT COUNT(*) FROM BOOKS.READING_GOALS rg WHERE rg.user_id = :userId " +
            "AND (rg.start_date + " +
            "  CASE rg.period " +
            "    WHEN '1d' THEN INTERVAL '1 day' " +
            "    WHEN '3d' THEN INTERVAL '3 days' " +
            "    WHEN 'week' THEN INTERVAL '1 week' " +
            "    WHEN 'month' THEN INTERVAL '1 month' " +
            "    WHEN 'quarter' THEN INTERVAL '3 months' " +
            "    WHEN 'year' THEN INTERVAL '1 year' " +
            "  END) < :currentDate", nativeQuery = true)
    Long countExpiredGoalsByUserId(@Param("userId") Integer userId,
                                   @Param("currentDate") LocalDateTime currentDate);

    void deleteByPgoalIdAndUserId(Integer pgoalId, Integer userId);
}