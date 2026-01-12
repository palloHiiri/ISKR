package com.fuzis.booksbackend.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookReadingStatusRequestDTO {
    @NotNull(message = "Book ID is required")
    private Integer bookId;

    @NotBlank(message = "Reading status is required")
    private String readingStatus; 
}