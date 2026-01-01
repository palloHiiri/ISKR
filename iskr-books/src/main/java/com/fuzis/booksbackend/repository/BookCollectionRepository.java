package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BookCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCollectionRepository extends JpaRepository<BookCollection, Integer> {

    List<BookCollection> findByConfidentiality(String confidentiality);

    @Query("SELECT bc FROM BookCollection bc LEFT JOIN FETCH bc.photoLink WHERE bc.bcolsId IN :ids")
    List<BookCollection> findByIdsWithPhotoLinks(@Param("ids") List<Integer> ids);

    Page<BookCollection> findByOwner_UserId(Integer userId, Pageable pageable);

    long countByOwner_UserId(Integer userId);

    // Получение коллекции с владельцем и фото
    @Query("SELECT bc FROM BookCollection bc " +
            "LEFT JOIN FETCH bc.owner " +
            "LEFT JOIN FETCH bc.photoLink " +
            "WHERE bc.bcolsId = :id")
    Optional<BookCollection> findByIdWithOwnerAndPhoto(@Param("id") Integer id);

    // Получение публичных коллекций
    @Query("SELECT bc FROM BookCollection bc WHERE bc.confidentiality = 'Public'")
    Page<BookCollection> findPublicCollections(Pageable pageable);
}