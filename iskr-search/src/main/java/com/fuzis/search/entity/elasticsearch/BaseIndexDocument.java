package com.fuzis.search.entity.elasticsearch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@Document(
        indexName = "global_search",
        createIndex = false
)
@Setting(
        settingPath = "/elasticsearch/settings.json"
)
public abstract class BaseIndexDocument {

    @Id
    protected String id;

    @Field(type = FieldType.Keyword)
    protected String type;

    @Field(type = FieldType.Date)
    protected LocalDate indexedAt;

    @Field(type = FieldType.Object)
    protected Map<String, Object> data;

    @Field(
            type = FieldType.Text,
            analyzer = "edge_ngram_analyzer",
            searchAnalyzer = "standard"
    )
    protected String searchText;
}