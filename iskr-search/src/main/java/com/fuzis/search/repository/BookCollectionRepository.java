package com.fuzis.search.repository;

import com.fuzis.search.entity.BookCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCollectionRepository extends JpaRepository<BookCollection, Integer> {

    @Query(value = "SELECT * FROM BOOKS.BOOK_COLLECTIONS WHERE confidentiality='Public' ORDER BY bcols_id",
            nativeQuery = true)
    List<BookCollection> findAllPublicOrderedById();

    @Query("SELECT bc FROM BookCollection bc " +
            "LEFT JOIN FETCH bc.photoLink pl " +
            "LEFT JOIN FETCH pl.imageData " +
            "WHERE bc.confidentiality = 'Public' " +
            "ORDER BY bc.bcolsId")
    Page<BookCollection> findAllPublicWithImages(Pageable pageable);

    @Query("SELECT bc.bcolsId FROM BookCollection bc " +
            "WHERE bc.confidentiality = 'Public' " +
            "ORDER BY bc.bcolsId")
    Page<Integer> findAllPublicCollectionIds(Pageable pageable);
}