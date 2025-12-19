package com.fuzis.search.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchItem {
    private String id;
    private String type;
    private Double score;
    private Map<String, Object> data;
    private Map<String, List<String>> highlights;
}
