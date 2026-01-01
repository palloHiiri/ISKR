package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookInCollectionDTO {
    private Integer bookId;
    private String title;
    private String subtitle;
    private String isbn;
    private Integer pageCnt;
    private String description;
    private ImageLinkDTO photoLink;
    private Double averageRating;
    private List<AuthorDTO> authors;
    private List<GenreDTO> genres;
}
