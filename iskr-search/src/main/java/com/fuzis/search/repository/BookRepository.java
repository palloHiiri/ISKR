package com.fuzis.search.repository;

import com.fuzis.search.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = "SELECT * FROM books.books ORDER BY id", nativeQuery = true)
    List<Book> findAllOrderedById();
}
