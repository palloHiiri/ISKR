package com.fuzis.search.repository.elasticsearch;

import com.fuzis.search.entity.elasticsearch.BaseIndexDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface SearchDocumentRepository extends
        ElasticsearchRepository<BaseIndexDocument, String> {

    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": [
                    "searchText^1.5",
                    "title^3",
                    "username^2",
                    "nickname^2",
                    "subtitle^2",
                    "description",
                    "isbn^5"
                  ],
                  "type": "best_fields",
                  "fuzziness": "AUTO"
                }
              }
            ],
            "filter": ?1
          }
        }
        """)
    Page<BaseIndexDocument> searchWithFilters(String query, String filterJson, Pageable pageable);

    Page<BaseIndexDocument> findBySearchTextContaining(String searchText, Pageable pageable);

    void deleteByType(String type);
}