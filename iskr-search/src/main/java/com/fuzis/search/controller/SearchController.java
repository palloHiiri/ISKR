package com.fuzis.search.controller;

import com.fuzis.search.service.SearchService;
import com.fuzis.search.transfer.SearchRequest;
import com.fuzis.search.transfer.SearchResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
            @RequestParam @Size(max = 100) String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer limit,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) Integer genreId) {

        // Если types не указаны, устанавливаем по умолчанию: user, collection, book
        if (types == null) {
            types = Arrays.asList("user", "collection", "book");
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .limit(limit)
                .types(types)
                .genreId(genreId)
                .build();

        SearchResult result = searchService.search(request);
        return ResponseEntity.ok(result);
    }
}