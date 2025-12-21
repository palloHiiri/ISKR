package com.fuzis.search.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "books_genres", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BooksGenre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gc_book_genre_id")
    private Integer gcBookGenreId;

    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "genre_id")
    private Integer genreId;
}