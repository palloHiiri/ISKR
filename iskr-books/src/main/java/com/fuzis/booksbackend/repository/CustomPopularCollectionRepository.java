package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BookCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomPopularCollectionRepository extends JpaRepository<BookCollection, Integer> {

    @Query(value = """
        SELECT bc.* FROM BOOKS.BOOK_COLLECTIONS bc
        LEFT JOIN BOOKS.LIKED_COLLECTIONS lc ON bc.bcols_id = lc.bcols_id
        WHERE bc.confidentiality = 'Public'
        GROUP BY bc.bcols_id
        ORDER BY COUNT(lc.lc_id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<BookCollection> findTopPopularCollections(@Param("limit") int limit);
}