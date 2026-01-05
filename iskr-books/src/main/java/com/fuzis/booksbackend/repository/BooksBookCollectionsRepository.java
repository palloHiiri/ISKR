package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BooksBookCollections;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface BooksBookCollectionsRepository extends JpaRepository<BooksBookCollections, Integer> {

    @Query("SELECT bbc.book.bookId, COUNT(bbc) as collectionsCount " +
            "FROM BooksBookCollections bbc " +
            "GROUP BY bbc.book.bookId " +
            "ORDER BY collectionsCount DESC")
    List<Object[]> findPopularBooks();

    @Query("SELECT bbc.bookCollection.bcolsId, COUNT(bbc) as bookCount " +
            "FROM BooksBookCollections bbc " +
            "WHERE bbc.bookCollection.bcolsId IN :collectionIds " +
            "GROUP BY bbc.bookCollection.bcolsId")
    List<Object[]> findBookCountsByCollectionIds(@Param("collectionIds") List<Integer> collectionIds);

    List<BooksBookCollections> findByBook_BookIdIn(List<Integer> bookIds);

    @Query("SELECT bbc FROM BooksBookCollections bbc " +
            "WHERE bbc.bookCollection.bcolsId = :collectionId")
    Page<BooksBookCollections> findByBookCollection_BcolsId(@Param("collectionId") Integer collectionId, Pageable pageable);

    // Добавляем метод для подсчета количества коллекций для книги
    @Query("SELECT COUNT(bbc) FROM BooksBookCollections bbc WHERE bbc.book.bookId = :bookId")
    long countByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT bbc FROM BooksBookCollections bbc WHERE bbc.bookCollection.bcolsId = :collectionId")
    List<BooksBookCollections> findByBookCollection_BcolsId(@Param("collectionId") Integer collectionId);

    // Подсчет количества книг в коллекции
    @Query("SELECT COUNT(bbc) FROM BooksBookCollections bbc WHERE bbc.bookCollection.bcolsId = :collectionId")
    long countByBookCollection_BcolsId(@Param("collectionId") Integer collectionId);
}