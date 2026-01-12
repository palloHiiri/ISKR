package com.fuzis.booksbackend.controller;

import com.fuzis.booksbackend.service.ReadingService;
import com.fuzis.booksbackend.transfer.*;
import com.fuzis.booksbackend.util.HttpUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingService readingService;
    private final HttpUtil httpUtil;

    @PostMapping("/status")
    public ResponseEntity<ChangeDTO<Object>> createBookReadingStatus(
            @RequestHeader Integer userId,
            @Valid @RequestBody BookReadingStatusRequestDTO requestDTO) {
        return httpUtil.handleServiceResponse(
                readingService.createBookReadingStatus(userId, requestDTO)
        );
    }

    @PutMapping("/status/{bookId}")
    public ResponseEntity<ChangeDTO<Object>> updateBookReadingStatus(
            @RequestHeader Integer userId,
            @PathVariable @Min(1) Integer bookId,
            @Valid @RequestBody UpdateReadingStatusRequestDTO requestDTO) {
        return httpUtil.handleServiceResponse(
                readingService.updateBookReadingStatus(userId, bookId, requestDTO)
        );
    }

    @GetMapping("/status/{bookId}")
    public ResponseEntity<ChangeDTO<Object>> getBookReadingStatus(
            @RequestHeader Integer userId,
            @PathVariable @Min(1) Integer bookId) {
        return httpUtil.handleServiceResponse(
                readingService.getBookReadingStatus(userId, bookId)
        );
    }

    @PutMapping("/progress/{bookId}")
    public ResponseEntity<ChangeDTO<Object>> updateReadingProgress(
            @RequestHeader Integer userId,
            @PathVariable @Min(1) Integer bookId,
            @Valid @RequestBody UpdateReadingProgressRequestDTO requestDTO) {
        return httpUtil.handleServiceResponse(
                readingService.updateReadingProgress(userId, bookId, requestDTO)
        );
    }

    @PostMapping("/goals")
    public ResponseEntity<ChangeDTO<Object>> createReadingGoal(
            @RequestHeader Integer userId,
            @Valid @RequestBody ReadingGoalRequestDTO requestDTO) {
        return httpUtil.handleServiceResponse(
                readingService.createReadingGoal(userId, requestDTO)
        );
    }

    @PutMapping("/goals/{goalId}")
    public ResponseEntity<ChangeDTO<Object>> updateReadingGoal(
            @RequestHeader Integer userId,
            @PathVariable @Min(1) Integer goalId,
            @Valid @RequestBody UpdateReadingGoalRequestDTO requestDTO) {
        return httpUtil.handleServiceResponse(
                readingService.updateReadingGoal(userId, goalId, requestDTO)
        );
    }

    @GetMapping("/goals")
    public ResponseEntity<ChangeDTO<Object>> getReadingGoals(
            @RequestHeader Integer userId) {
        return httpUtil.handleServiceResponse(
                readingService.getReadingGoals(userId)
        );
    }

    @DeleteMapping("/goals/{goalId}")
    public ResponseEntity<ChangeDTO<Object>> deleteReadingGoal(
            @RequestHeader Integer userId,
            @PathVariable @Min(1) Integer goalId) {
        return httpUtil.handleServiceResponse(
                readingService.deleteReadingGoal(userId, goalId)
        );
    }

    @GetMapping("/goals/stats")
    public ResponseEntity<ChangeDTO<Object>> getGoalStats(
            @RequestHeader Integer userId) {
        return httpUtil.handleServiceResponse(
                readingService.getGoalStats(userId)
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<ChangeDTO<Object>> getAccountStats(
            @RequestHeader Integer userId) {
        return httpUtil.handleServiceResponse(
                readingService.getAccountStats(userId)
        );
    }
}