package com.fuzis.booksbackend.transfer;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdateDTO {
    private String title;

    @Min(value = 1, message = "Page count must be at least 1")
    private Integer pageCnt;

    private String subtitle;
    private String description;
    private String isbn;
    private Integer photoLink;
    private Set<Integer> authorIds;
    private Set<Integer> genreIds;
}