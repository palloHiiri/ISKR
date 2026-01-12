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

    @Query("SELECT bc FROM BookCollection bc " +
            "LEFT JOIN FETCH bc.owner " +
            "LEFT JOIN FETCH bc.photoLink " +
            "WHERE bc.bcolsId = :id")
    Optional<BookCollection> findByIdWithOwnerAndPhoto(@Param("id") Integer id);

    @Query("SELECT bc FROM BookCollection bc WHERE bc.confidentiality = 'Public'")
    Page<BookCollection> findPublicCollections(Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(bc) > 0 THEN true ELSE false END " +
            "FROM BookCollection bc WHERE bc.owner.userId = :userId AND bc.collectionType = 'Wishlist'")
    boolean existsWishlistByUserId(@Param("userId") Integer userId);

    @Query("SELECT bc FROM BookCollection bc " +
            "LEFT JOIN FETCH bc.photoLink " +
            "WHERE bc.owner.userId = :userId AND bc.collectionType = 'Wishlist'")
    Optional<BookCollection> findWishlistByUserId(@Param("userId") Integer userId);

    @Query("SELECT bc FROM BookCollection bc WHERE bc.owner.userId = :userId AND bc.collectionType = 'Wishlist'")
    List<BookCollection> findWishlistsByUserId(@Param("userId") Integer userId);

    boolean existsById(Integer collectionId);
}