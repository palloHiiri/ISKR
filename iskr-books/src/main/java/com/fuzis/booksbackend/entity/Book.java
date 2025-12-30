package com.fuzis.booksbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "BOOKS", schema = "BOOKS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "isbn", unique = true, length = 17)
    private String isbn;

    @Column(name = "title", nullable = false, length = 1024)
    private String title;

    @Column(name = "subtitle", length = 1024)
    private String subtitle;

    @Column(name = "description")
    private String description;

    @Column(name = "page_cnt", nullable = false)
    private Integer pageCnt;

    @OneToOne
    @JoinColumn(name = "photo_link")
    private ImageLink photoLink;

    @ManyToOne
    @JoinColumn(name = "added_by")
    private User addedBy;

    @ManyToMany
    @JoinTable(
            name = "BOOKS_AUTHORS",
            schema = "BOOKS",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "BOOKS_GENRES",
            schema = "BOOKS",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();
}