package com.fuzis.search.entity.elasticsearch;

import com.fuzis.search.entity.Book;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class BookDocument extends BaseIndexDocument {
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String title;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String subtitle;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String description;

    @Field(type = FieldType.Keyword)
    private String isbn;

    public static BookDocument fromEntity(Book book) {
        BookDocument doc = new BookDocument();
        doc.setId("book_" + book.getBookId());
        doc.setType("book");
        doc.setIndexedAt(LocalDate.now());
        doc.setTitle(book.getTitle());
        doc.setSubtitle(book.getSubtitle());
        doc.setIsbn(book.getIsbn());
        doc.setDescription(book.getDescription());

        Map<String, Object> data = new HashMap<>();
        data.put("id", book.getBookId());
        data.put("title", book.getTitle());
        data.put("subtitle", book.getSubtitle());
        data.put("isbn", book.getIsbn());
        data.put("description", book.getDescription());
        data.put("page_cnt", book.getPageCnt());
        doc.setData(data);

        doc.setSearchText(
                (book.getTitle() != null ? book.getTitle() + " " : "") +
                        (book.getSubtitle() != null ? book.getSubtitle() + " " : "") +
                        (book.getIsbn() != null ? book.getIsbn() + " " : "") +
                        (book.getDescription() != null ? book.getDescription() : "")
        );

        return doc;
    }
}
