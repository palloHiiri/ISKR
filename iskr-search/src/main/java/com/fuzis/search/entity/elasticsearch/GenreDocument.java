package com.fuzis.search.entity.elasticsearch;

import com.fuzis.search.entity.Genre;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class GenreDocument extends BaseIndexDocument {
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String name;

    public static GenreDocument fromEntity(Genre genre) {
        GenreDocument doc = new GenreDocument();
        doc.setId("genre_" + genre.getGenreId());
        doc.setType("genre");
        doc.setIndexedAt(LocalDate.now());
        doc.setName(genre.getName());

        Map<String, Object> data = new HashMap<>();
        data.put("id", genre.getGenreId());
        data.put("name", genre.getName());
        doc.setData(data);

        doc.setSearchText(genre.getName() != null ? genre.getName() : "");

        return doc;
    }
}