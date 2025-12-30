package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomPopularUserRepository extends JpaRepository<User, Integer> {

    @Query(value = """
        SELECT u.* FROM ACCOUNTS.USERS u
        LEFT JOIN BOOKS.SUBSCRIBERS s ON u.user_id = s.subs_user_on_id
        GROUP BY u.user_id
        ORDER BY COUNT(s.subs_id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<User> findTopPopularUsers(@Param("limit") int limit);
}