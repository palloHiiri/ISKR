package com.fuzis.booksbackend.transfer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 1024, message = "Title must not exceed 1024 characters")
    private String title;

    @NotNull(message = "Page count is required")
    @Min(value = 1, message = "Page count must be at least 1")
    private Integer pageCnt;

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be at least 1")
    private Integer addedBy;

    private Set<Integer> authorIds;
    
    private Set<Integer> genreIds;

    @Size(max = 1024, message = "Subtitle must not exceed 1024 characters")
    private String subtitle;

    private String description;

    @Size(max = 17, message = "ISBN must not exceed 17 characters")
    private String isbn;

    @Min(value = 1, message = "Photo link must be a positive integer")
    private Integer photoLink;
}