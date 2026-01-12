package com.fuzis.booksbackend.transfer;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReadingGoalRequestDTO {
    @NotBlank(message = "Period is required")
    private String period; 

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Integer amount;

    @NotBlank(message = "Goal type is required")
    private String goalType; 
}