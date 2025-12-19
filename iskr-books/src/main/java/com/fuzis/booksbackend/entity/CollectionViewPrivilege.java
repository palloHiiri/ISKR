package com.fuzis.booksbackend.entity;

import com.fuzis.booksbackend.entity.enumerate.CvpStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "collection_view_privileges", schema = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionViewPrivilege {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cvp_id")
    private Integer cvpId;

    @Column(name = "bcols_id")
    private Integer bcolsId;

    @Column(name = "user_id")
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CvpStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bcols_id", insertable = false, updatable = false)
    private BookCollection collection;
}