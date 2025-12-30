package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH b.genres " +
            "WHERE b.bookId = :id")
    Optional<Book> findByIdWithAuthorsAndGenres(@Param("id") Integer id);

    @Query("SELECT b FROM Book b " +
            "LEFT JOIN FETCH b.authors " +
            "LEFT JOIN FETCH b.genres")
    Page<Book> findAllWithAuthorsAndGenres(Pageable pageable);

    boolean existsByIsbn(String isbn);

    boolean existsByPhotoLink_ImglId(Integer photoLink);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Book b WHERE b.title = :title AND " +
            "(b.subtitle = :subtitle OR (:subtitle IS NULL AND b.subtitle IS NULL))")
    boolean existsByTitleAndSubtitle(@Param("title") String title,
                                     @Param("subtitle") String subtitle);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Book b WHERE b.title = :title AND " +
            "(b.subtitle = :subtitle OR (:subtitle IS NULL AND b.subtitle IS NULL)) " +
            "AND b.bookId != :bookId")
    boolean existsByTitleAndSubtitleAndBookIdNot(@Param("title") String title,
                                                 @Param("subtitle") String subtitle,
                                                 @Param("bookId") Integer bookId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Book b WHERE b.photoLink.imglId = :photoLink AND b.bookId != :bookId")
    boolean existsByPhotoLinkAndBookIdNot(@Param("photoLink") Integer photoLink,
                                          @Param("bookId") Integer bookId);
}