package com.fuzis.search.repository;

import com.fuzis.search.entity.BooksBookCollections;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BooksBookCollectionsRepository extends JpaRepository<BooksBookCollections, Integer> {

    @Query("SELECT bbc.book.bookId, COUNT(bbc) as collectionsCount " +
            "FROM BooksBookCollections bbc " +
            "WHERE bbc.book.bookId IN :bookIds " +
            "GROUP BY bbc.book.bookId")
    List<Object[]> findCollectionsCountByBookIds(@Param("bookIds") List<Integer> bookIds);

    @Query("SELECT bbc.bookCollection.bcolsId, COUNT(bbc) as bookCount " +
            "FROM BooksBookCollections bbc " +
            "WHERE bbc.bookCollection.bcolsId IN :collectionIds " +
            "GROUP BY bbc.bookCollection.bcolsId")
    List<Object[]> findBookCountByCollectionIds(@Param("collectionIds") List<Integer> collectionIds);
}