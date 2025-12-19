package com.fuzis.booksbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "liked_collections", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikedCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lc_id")
    private Integer lcId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "bcols_id")
    private Integer bcolsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bcols_id", insertable = false, updatable = false)
    private BookCollection collection;
}