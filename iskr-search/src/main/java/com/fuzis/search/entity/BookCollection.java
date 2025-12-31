package com.fuzis.search.entity;

import com.fuzis.search.entity.enumerate.CollectionType;
import com.fuzis.search.entity.enumerate.Confidentiality;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BOOK_COLLECTIONS", schema = "BOOKS")
@Data
public class BookCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bcols_id")
    private Integer bcolsId;

    @Column(name = "owner_id", nullable = false)
    private Integer ownerId;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality", nullable = false)
    private Confidentiality confidentiality;

    @OneToOne
    @JoinColumn(name = "photo_link")
    private ImageLinks photoLink;

    @Transient
    private Long likesCount;

    @Transient
    private Integer bookCount;
}