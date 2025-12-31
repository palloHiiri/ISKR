package com.fuzis.search.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "IMAGE_LINKS", schema = "IMAGES")
@Data
public class ImageLinks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imgl_id")
    private Integer imglId;

    @OneToOne
    @JoinColumn(name = "imgd_id")
    private ImageDatas imageData;
}
