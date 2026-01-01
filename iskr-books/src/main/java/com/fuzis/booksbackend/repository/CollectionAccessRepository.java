package com.fuzis.booksbackend.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionAccessRepository {

    @Query(value = "SELECT BOOKS.CAN_VIEW_COLLECTION(:userId, :collectionId)", nativeQuery = true)
    Boolean canViewCollection(@Param("userId") Integer userId, @Param("collectionId") Integer collectionId);
}