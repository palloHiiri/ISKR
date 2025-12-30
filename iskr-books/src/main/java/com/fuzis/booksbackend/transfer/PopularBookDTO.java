package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularBookDTO {
    private Integer bookId;
    private String title;
    private String subtitle;
    private String isbn;
    private Integer pageCnt;
    private Long collectionsCount;
    private Double averageRating; // Новое поле: средний рейтинг
    private ImageLinkDTO photoLink;
}