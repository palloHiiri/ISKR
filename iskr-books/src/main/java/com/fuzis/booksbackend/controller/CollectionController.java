package com.fuzis.booksbackend.controller;

import com.fuzis.booksbackend.service.CollectionService;
import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.util.HttpUtil;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;
    private final HttpUtil httpUtil;

    @GetMapping("/{collectionId}")
    public ResponseEntity<ChangeDTO<Object>> getCollectionDetail(
            @PathVariable @Min(1) Integer collectionId,
            @RequestParam(required = false) Integer userId) {
        return httpUtil.handleServiceResponse(collectionService.getCollectionDetail(collectionId, userId));
    }

    @GetMapping("/{collectionId}/books")
    public ResponseEntity<ChangeDTO<Object>> getCollectionBooks(
            @PathVariable @Min(1) Integer collectionId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer batch) {
        return httpUtil.handleServiceResponse(
                collectionService.getCollectionBooks(collectionId, userId, page, batch)
        );
    }
}