package com.fuzis.booksbackend.controller;

import com.fuzis.booksbackend.service.PopularService;
import com.fuzis.booksbackend.transfer.ChangeDTO;
import com.fuzis.booksbackend.util.HttpUtil;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/popular")
@RequiredArgsConstructor
public class PopularController {

    private final PopularService popularService;
    private final HttpUtil httpUtil;

    @GetMapping("/users")
    public ResponseEntity<ChangeDTO<Object>> getPopularUsers(
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        return httpUtil.handleServiceResponse(popularService.getPopularUsers(limit));
    }

    @GetMapping("/collections")
    public ResponseEntity<ChangeDTO<Object>> getPopularCollections(
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        return httpUtil.handleServiceResponse(popularService.getPopularCollections(limit));
    }

    @GetMapping("/books")
    public ResponseEntity<ChangeDTO<Object>> getPopularBooks(
            @RequestParam(defaultValue = "10") @Min(1) Integer limit) {
        return httpUtil.handleServiceResponse(popularService.getPopularBooks(limit));
    }
}