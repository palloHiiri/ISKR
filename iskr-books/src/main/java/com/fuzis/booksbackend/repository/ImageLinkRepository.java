package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.ImageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageLinkRepository extends JpaRepository<ImageLink, Integer> {

    @Query("SELECT il FROM ImageLink il LEFT JOIN FETCH il.imageData WHERE il.imglId IN :ids")
    List<ImageLink> findByIdsWithImageData(@Param("ids") List<Integer> ids);
}