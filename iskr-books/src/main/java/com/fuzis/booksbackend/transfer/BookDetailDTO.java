package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailDTO {
    private Integer bookId;
    private String isbn;
    private String title;
    private String subtitle;
    private String description;
    private Integer pageCnt;
    private ImageLinkDTO photoLink;
    private UserDTO addedBy;
    private List<AuthorDetailDTO> authors;
    private List<GenreDetailDTO> genres;
    private Long collectionsCount; 
    private Double averageRating; 
    private Integer reviewsCount; 
}