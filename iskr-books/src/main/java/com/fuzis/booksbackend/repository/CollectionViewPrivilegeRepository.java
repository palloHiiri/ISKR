package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.CollectionViewPrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionViewPrivilegeRepository extends JpaRepository<CollectionViewPrivilege, Integer> {

    @Query("SELECT cvp FROM CollectionViewPrivilege cvp WHERE cvp.bcolsId = :collectionId AND cvp.userId = :userId")
    Optional<CollectionViewPrivilege> findByCollectionIdAndUserId(@Param("collectionId") Integer collectionId,
                                                                  @Param("userId") Integer userId);

    @Query("SELECT COUNT(cvp) > 0 FROM CollectionViewPrivilege cvp WHERE cvp.bcolsId = :collectionId AND cvp.userId = :userId")
    boolean existsByCollectionIdAndUserId(@Param("collectionId") Integer collectionId,
                                          @Param("userId") Integer userId);

    @Query("DELETE FROM CollectionViewPrivilege cvp WHERE cvp.bcolsId = :collectionId AND cvp.userId = :userId")
    void deleteByCollectionIdAndUserId(@Param("collectionId") Integer collectionId,
                                       @Param("userId") Integer userId);

    @Query("SELECT cvp FROM CollectionViewPrivilege cvp WHERE cvp.bcolsId = :collectionId")
    List<CollectionViewPrivilege> findByBcolsId(@Param("collectionId") Integer collectionId);
}