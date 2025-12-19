package com.fuzis.booksbackend.entity;

import com.fuzis.booksbackend.entity.enumerate.CollectionType;
import com.fuzis.booksbackend.entity.enumerate.Confidentiality;
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
@Table(name = "book_collections", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"books", "viewPrivileges", "likes"})
@EqualsAndHashCode(exclude = {"books", "viewPrivileges", "likes"})
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

    @ManyToMany
    @JoinTable(
            name = "books_book_collections",
            schema = "books",
            joinColumns = @JoinColumn(name = "bcols_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> books;

    @OneToMany(mappedBy = "collection")
    private Set<CollectionViewPrivilege> viewPrivileges;

    @OneToMany(mappedBy = "collection")
    private Set<LikedCollection> likes;
}