    package com.fuzis.search.entity.elasticsearch;

    import com.fuzis.search.entity.BookCollection;
    import lombok.Getter;
    import lombok.Setter;
    import org.springframework.data.elasticsearch.annotations.*;

    import java.time.LocalDate;
    import java.util.HashMap;
    import java.util.Map;

    @Getter
    @Setter
    public class BookCollectionDocument extends BaseIndexDocument {
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
        private String description;

        @Field(type = FieldType.Integer)
        private Integer ownerId;

        public static BookCollectionDocument fromEntity(BookCollection collection) {
            BookCollectionDocument doc = new BookCollectionDocument();
            doc.setId("collection_" + collection.getBcolsId());
            doc.setType("collection");
            doc.setIndexedAt(LocalDate.now());
            doc.setTitle(collection.getTitle());
            doc.setDescription(collection.getDescription());
            doc.setOwnerId(collection.getOwnerId());

            Map<String, Object> data = new HashMap<>();
            data.put("id", collection.getBcolsId());
            data.put("title", collection.getTitle());
            data.put("description", collection.getDescription());
            data.put("ownerId", collection.getOwnerId());
            doc.setData(data);

            doc.setSearchText(
                    (collection.getTitle() != null ? collection.getTitle() + " " : "") +
                    (collection.getDescription() != null ? collection.getDescription() + " ": "") +
                    (collection.getOwnerId() != null ? collection.getOwnerId(): "")
            );

            return doc;
        }
    }
