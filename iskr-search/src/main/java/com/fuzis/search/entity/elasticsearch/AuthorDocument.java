package com.fuzis.search.entity.elasticsearch;

import com.fuzis.search.entity.Author;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class AuthorDocument extends BaseIndexDocument {
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String name;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String realName;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Date)
    private LocalDate birthDate;

    public static AuthorDocument fromEntity(Author author) {
        AuthorDocument doc = new AuthorDocument();
        doc.setId("author_" + author.getAuthorId());
        doc.setType("author");
        doc.setIndexedAt(LocalDate.now());
        doc.setName(author.getName());
        doc.setRealName(author.getRealName());
        doc.setDescription(author.getDescription());
        if (author.getBirthDate() != null) {
            doc.setBirthDate(author.getBirthDate().toLocalDate());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", author.getAuthorId());
        data.put("name", author.getName());
        data.put("realName", author.getRealName());
        data.put("description", author.getDescription());
        data.put("birthDate", author.getBirthDate());
        doc.setData(data);

        // Собираем searchText из полей, которые мы хотим индексировать для поиска
        StringBuilder searchTextBuilder = new StringBuilder();
        if (author.getName() != null) {
            searchTextBuilder.append(author.getName()).append(" ");
        }
        if (author.getRealName() != null) {
            searchTextBuilder.append(author.getRealName()).append(" ");
        }
        if (author.getDescription() != null) {
            searchTextBuilder.append(author.getDescription()).append(" ");
        }
        doc.setSearchText(searchTextBuilder.toString());

        return doc;
    }
}