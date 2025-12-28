package com.fuzis.booksbackend.repository;

import com.fuzis.booksbackend.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Integer> {
    List<Author> findByAuthorIdIn(List<Integer> ids);
}