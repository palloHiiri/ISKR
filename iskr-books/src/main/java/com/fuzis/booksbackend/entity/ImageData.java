package com.fuzis.booksbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "IMAGE_DATAS", schema = "IMAGES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imgd_id")
    private Integer imgdId;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @Column(name = "size", nullable = false)
    private Integer size;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "extension", nullable = false, length = 10)
    private String extension;
}