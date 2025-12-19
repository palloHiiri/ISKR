package com.fuzis.search.entity;

import com.fuzis.search.entity.enumerate.CollectionType;
import com.fuzis.search.entity.enumerate.Confidentiality;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "book_collections", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bcols_id")
    private Integer bcolsId;

    @Column(name = "owner_id")
    private Integer ownerId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidentiality")
    private Confidentiality confidentiality;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_collection_type")
    private CollectionType bookCollectionType;
}