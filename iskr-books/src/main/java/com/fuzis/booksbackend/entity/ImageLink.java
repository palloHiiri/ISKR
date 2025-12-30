package com.fuzis.booksbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "IMAGE_LINKS", schema = "IMAGES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imgl_id")
    private Integer imglId;

    @ManyToOne
    @JoinColumn(name = "imgd_id")
    private ImageData imageData;
}