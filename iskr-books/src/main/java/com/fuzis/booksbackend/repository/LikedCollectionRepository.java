package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.LikedCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LikedCollectionRepository extends JpaRepository<LikedCollection, Integer> {

    @Query("SELECT lc.bcols.bcolsId, COUNT(lc) as likesCount " +
            "FROM LikedCollection lc " +
            "JOIN lc.bcols bc " +
            "WHERE bc.confidentiality = 'Public' " +
            "GROUP BY lc.bcols.bcolsId " +
            "ORDER BY likesCount DESC")
    List<Object[]> findPopularCollections();

    List<LikedCollection> findByBcols_BcolsIdIn(List<Integer> collectionIds);
}