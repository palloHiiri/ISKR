package com.fuzis.booksbackend.entity;

import com.fuzis.booksbackend.entity.enumerate.CollectionType;
import com.fuzis.booksbackend.entity.enumerate.Confidentiality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "BOOK_COLLECTIONS", schema = "BOOKS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bcols_id")
    private Integer bcolsId;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality", nullable = false)
    private Confidentiality confidentiality;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_collection_type", nullable = false)
    private CollectionType collectionType;

    @OneToOne
    @JoinColumn(name = "photo_link")
    private ImageLink photoLink;
}