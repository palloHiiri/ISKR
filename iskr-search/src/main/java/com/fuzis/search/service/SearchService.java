package com.fuzis.search.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.fuzis.search.entity.elasticsearch.BaseIndexDocument;
import com.fuzis.search.exception.SearchException;
import com.fuzis.search.transfer.SearchRequest;
import com.fuzis.search.transfer.SearchResult;
import com.fuzis.search.transfer.SearchItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchService {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public SearchResult search(SearchRequest request) {
        log.info("Search request: {}", request);

        try {
            NativeQuery searchQuery = buildSearchQuery(request);

            SearchHits<BaseIndexDocument> searchHits = elasticsearchOperations.search(
                    searchQuery,
                    BaseIndexDocument.class
            );

            List<SearchItem> items = searchHits.stream()
                    .map(hit -> {
                        BaseIndexDocument document = hit.getContent();
                        return SearchItem.builder()
                                .id(document.getId())
                                .type(document.getType())
                                .score((double) hit.getScore())
                                .data(document.getData())
                                .build();
                    })
                    .collect(Collectors.toList());

            return SearchResult.builder()
                    .query(request.getQuery())
                    .total(searchHits.getTotalHits())
                    .limit(request.getLimit())
                    .items(items)
                    .took(0.0)
                    .build();

        } catch (Exception e) {
            log.error("Search error: ", e);
            throw new SearchException("Search error: ", e);
        }
    }

    private NativeQuery buildSearchQuery(SearchRequest request) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            MultiMatchQuery.Builder multiMatchBuilder = new MultiMatchQuery.Builder()
                    .query(request.getQuery())
                    .fields(List.of(
                            "searchText^1.5",
                            "title^3",
                            "username^2",
                            "nickname^2",
                            "subtitle^2",
                            "description^1",
                            "isbn^5"
                    ))
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO");

            boolQueryBuilder.must(multiMatchBuilder.build()._toQuery());
        }

        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            TermsQuery.Builder termsBuilder = new TermsQuery.Builder()
                    .field("type")
                    .terms(t -> t.value(request.getTypes().stream()
                            .map(v -> FieldValue.of(v))
                            .collect(Collectors.toList())));
            boolQueryBuilder.filter(termsBuilder.build()._toQuery());
        }

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(PageRequest.of(0, request.getLimit()));

        return queryBuilder.build();
    }
}