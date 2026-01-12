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
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
public class AdminCollectionController {

    private final CollectionService collectionService;
    private final HttpUtil httpUtil;

    private static final Integer ADMIN_USER_ID = null; 

    @GetMapping("/{collectionId}")
    public ResponseEntity<ChangeDTO<Object>> getCollectionDetail(
            @PathVariable @Min(1) Integer collectionId) {
        return httpUtil.handleServiceResponse(
                collectionService.getCollectionDetail(collectionId, ADMIN_USER_ID)
        );
    }

    @GetMapping("/{collectionId}/books")
    public ResponseEntity<ChangeDTO<Object>> getCollectionBooks(
            @PathVariable @Min(1) Integer collectionId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) Integer batch) {
        return httpUtil.handleServiceResponse(
                collectionService.getCollectionBooks(collectionId, ADMIN_USER_ID, page, batch)
        );
    }
}