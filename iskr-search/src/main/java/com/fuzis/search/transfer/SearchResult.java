package com.fuzis.search.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private String query;
    private Long total;
    private Integer limit;
    private List<SearchItem> items;
    private Double took;
}
