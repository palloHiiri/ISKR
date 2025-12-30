package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.BookCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookCollectionRepository extends JpaRepository<BookCollection, Integer> {

    List<BookCollection> findByConfidentiality(String confidentiality);

    @Query("SELECT bc FROM BookCollection bc LEFT JOIN FETCH bc.photoLink WHERE bc.bcolsId IN :ids")
    List<BookCollection> findByIdsWithPhotoLinks(@Param("ids") List<Integer> ids);
}