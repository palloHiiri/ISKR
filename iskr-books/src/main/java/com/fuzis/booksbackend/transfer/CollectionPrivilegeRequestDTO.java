package com.fuzis.booksbackend.transfer;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollectionPrivilegeRequestDTO {
    @NotNull(message = "User ID is required")
        private Integer userId;

    @NotNull(message = "Status is required")
    private String cvpStatus; 
}