package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    List<Genre> findByGenreIdIn(List<Integer> ids);
}