package com.fuzis.booksbackend.transfer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCreateDTO {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Page count is required")
    @Min(value = 1, message = "Page count must be at least 1")
    private Integer pageCnt;

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be at least 1")
    private Integer addedBy;

    @NotEmpty(message = "At least one author is required")
    private Set<Integer> authorIds;

    @NotEmpty(message = "At least one genre is required")
    private Set<Integer> genreIds;

    private String subtitle;
    private String description;
    private String isbn;
    private Integer photoLink;
}