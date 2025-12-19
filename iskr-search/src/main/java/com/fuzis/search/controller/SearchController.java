package com.fuzis.search.controller;

import com.fuzis.search.service.SearchService;
import com.fuzis.search.transfer.SearchRequest;
import com.fuzis.search.transfer.SearchResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService){
        this.searchService = searchService;
    }

    @PostMapping("/query")
    public ResponseEntity<SearchResult> search(
            @RequestParam @NotBlank @Size(min = 1, max = 100) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit,
            @RequestParam(required = false) List<String> types) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .limit(limit)
                .types(types != null ? types : Collections.emptyList())
                .build();

        SearchResult result = searchService.search(request);
        return ResponseEntity.ok(result);
    }

}
