package com.fuzis.search.repository;

import com.fuzis.search.entity.BookCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookCollectionRepository extends JpaRepository<BookCollection, Long> {
    @Query(value = "SELECT * FROM books.book_collections ORDER BY id", nativeQuery = true)
    List<BookCollection> findAllOrderedById();
}
