package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BooksBookCollections;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BooksBookCollectionsRepository extends JpaRepository<BooksBookCollections, Integer> {

    @Query("SELECT bbc.book.bookId, COUNT(bbc) as collectionsCount " +
            "FROM BooksBookCollections bbc " +
            "GROUP BY bbc.book.bookId " +
            "ORDER BY collectionsCount DESC")
    List<Object[]> findPopularBooks();

    List<BooksBookCollections> findByBook_BookIdIn(List<Integer> bookIds);
}