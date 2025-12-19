package com.fuzis.booksbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "books_book_collections", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BooksBookCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "c_book_bcol_id")
    private Integer cBookBcolId;

    @Column(name = "book_id")
    private Integer bookId;

    @Column(name = "bcols_id")
    private Integer bcolsId;
}