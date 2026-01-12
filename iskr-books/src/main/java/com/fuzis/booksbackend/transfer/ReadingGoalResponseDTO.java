package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadingGoalResponseDTO {
    private Integer pgoalId;
    private Integer userId;
    private String period;
    private LocalDateTime startDate;
    private Integer amount;
    private String goalType;
    private Integer currentProgress = 0; 
    private Boolean isCompleted;
    private LocalDateTime endDate;
}