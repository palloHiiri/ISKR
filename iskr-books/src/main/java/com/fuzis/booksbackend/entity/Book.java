package com.fuzis.booksbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Entity
@Table(name = "books", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"authors", "genres", "reviews", "readingStatuses", "collections"})
@EqualsAndHashCode(exclude = {"authors", "genres", "reviews", "readingStatuses", "collections"})
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "isbn")
    private String isbn;

    @Column(name = "title")
    private String title;

    @Column(name = "subtitle")
    private String subtitle;

    @Column(name = "description")
    private String description;

    @Column(name = "page_cnt")
    private Integer pageCnt;

    @Column(name = "photo_link")
    private Integer photoLink;

    @Column(name = "added_by")
    private Integer addedBy;

    @ManyToMany
    @JoinTable(
            name = "books_authors",
            schema = "books",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    private Set<Author> authors;

    @ManyToMany
    @JoinTable(
            name = "books_genres",
            schema = "books",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres;

    @OneToMany(mappedBy = "book")
    private Set<BookReview> reviews;

    @OneToMany(mappedBy = "book")
    private Set<BookReadingStatus> readingStatuses;

    @ManyToMany(mappedBy = "books")
    private Set<BookCollection> collections;
}