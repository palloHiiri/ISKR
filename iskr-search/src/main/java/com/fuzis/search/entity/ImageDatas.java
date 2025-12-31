package com.fuzis.search.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "IMAGE_DATAS", schema = "IMAGES")
@Data
public class ImageDatas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imgd_id")
    private Integer imgdId;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "size", nullable = false)
    private Integer size;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "extension", nullable = false, length = 10)
    private String extension;
}
