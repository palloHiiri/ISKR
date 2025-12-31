package com.fuzis.search.entity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity
@Table(name = "BOOKS", schema = "BOOKS")
@Data
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "isbn", length = 17)
    private String isbn;

    @Column(name = "title", nullable = false, length = 1024)
    private String title;

    @Column(name = "subtitle", length = 1024)
    private String subtitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "page_cnt", nullable = false)
    private Integer pageCnt;

    @OneToOne
    @JoinColumn(name = "photo_link")
    private ImageLinks photoLink;

    @Column(name = "added_by")
    private Integer addedBy;

    @ManyToMany
    @JoinTable(
            name = "BOOKS_GENRES",
            schema = "BOOKS",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;

    @ManyToMany
    @JoinTable(
            name = "BOOKS_AUTHORS",
            schema = "BOOKS",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private List<Author> authors;

    @Transient
    private Double averageRating;

    @Transient
    private Long collectionsCount;
}