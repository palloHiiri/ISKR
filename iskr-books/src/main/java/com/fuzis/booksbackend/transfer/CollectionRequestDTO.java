package com.fuzis.booksbackend.transfer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CollectionRequestDTO {
    @NotNull(message = "Title is required", groups = {CreateValidation.class})
    @Size(min = 1, max = 512, message = "Title must be between 1 and 512 characters")
    private String title;

    @Size(max = 4000, message = "Description must be at most 4000 characters")
    private String description;

    @NotNull(message = "Confidentiality is required", groups = {CreateValidation.class})
    private String confidentiality; 

    @NotNull(message = "Collection type is required", groups = {CreateValidation.class})
    private String collectionType; 

    private Integer photoLink;

    public interface CreateValidation {}
    public interface UpdateValidation {}
}