package com.fuzis.search.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books_authors", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BooksAuthor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gc_book_author_id")
    private Integer gcBookAuthorId;

    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "author_id")
    private Integer authorId;
}