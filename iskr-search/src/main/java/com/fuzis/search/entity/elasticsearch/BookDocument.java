package com.fuzis.search.entity.elasticsearch;

import com.fuzis.search.entity.Author;
import com.fuzis.search.entity.Book;
import com.fuzis.search.entity.Genre;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Field(type = FieldType.Integer)
    private Integer pageCnt;

    @Field(type = FieldType.Integer)
    private Integer addedBy;

    @Field(type = FieldType.Integer)
    private List<Integer> genreIds;

    @Field(type = FieldType.Integer)
    private List<Integer> authorIds;

    @Field(type = FieldType.Double)
    private Double averageRating;

    @Field(type = FieldType.Long)
    private Long collectionsCount;

    @Field(type = FieldType.Keyword)
    private String imageUuid;

    @Field(type = FieldType.Keyword)
    private String imageExtension;

    public static BookDocument fromEntity(Book book) {
        BookDocument doc = new BookDocument();
        doc.setId("book_" + book.getBookId());
        doc.setType("book");
        doc.setIndexedAt(LocalDate.now());
        doc.setTitle(book.getTitle());
        doc.setSubtitle(book.getSubtitle());
        doc.setDescription(book.getDescription());
        doc.setIsbn(book.getIsbn());
        doc.setPageCnt(book.getPageCnt());
        doc.setAddedBy(book.getAddedBy());
        doc.setAverageRating(book.getAverageRating());
        doc.setCollectionsCount(book.getCollectionsCount());

        if (book.getGenres() != null) {
            doc.setGenreIds(book.getGenres().stream()
                    .map(Genre::getGenreId)
                    .collect(Collectors.toList()));
        }

        if (book.getAuthors() != null) {
            doc.setAuthorIds(book.getAuthors().stream()
                    .map(Author::getAuthorId)
                    .collect(Collectors.toList()));
        }

        if (book.getPhotoLink() != null && book.getPhotoLink().getImageData() != null) {
            doc.setImageUuid(book.getPhotoLink().getImageData().getUuid());
            doc.setImageExtension(book.getPhotoLink().getImageData().getExtension());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", book.getBookId());
        data.put("title", book.getTitle());
        data.put("subtitle", book.getSubtitle());
        data.put("description", book.getDescription());
        data.put("isbn", book.getIsbn());
        data.put("pageCnt", book.getPageCnt());
        data.put("addedBy", book.getAddedBy());
        data.put("averageRating", book.getAverageRating());
        data.put("collectionsCount", book.getCollectionsCount());

        if (doc.getImageUuid() != null) {
            data.put("imageUuid", doc.getImageUuid());
            data.put("imageExtension", doc.getImageExtension());
        }

        if (book.getGenres() != null) {
            data.put("genreIds", doc.getGenreIds());
            data.put("genres", book.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.toList()));
        }

        if (book.getAuthors() != null) {
            data.put("authorIds", doc.getAuthorIds());
            data.put("authors", book.getAuthors().stream()
                    .map(Author::getName)
                    .collect(Collectors.toList()));
        }

        doc.setData(data);

        StringBuilder searchTextBuilder = new StringBuilder();
        if (book.getTitle() != null) searchTextBuilder.append(book.getTitle()).append(" ");
        if (book.getSubtitle() != null) searchTextBuilder.append(book.getSubtitle()).append(" ");
        if (book.getDescription() != null) searchTextBuilder.append(book.getDescription()).append(" ");
        if (book.getIsbn() != null) searchTextBuilder.append(book.getIsbn()).append(" ");

        if (book.getAuthors() != null) {
            for (Author author : book.getAuthors()) {
                if (author.getName() != null) {
                    searchTextBuilder.append(author.getName()).append(" ");
                }
                if (author.getRealName() != null) {
                    searchTextBuilder.append(author.getRealName()).append(" ");
                }
            }
        }

        if (book.getGenres() != null) {
            for (Genre genre : book.getGenres()) {
                if (genre.getName() != null) {
                    searchTextBuilder.append(genre.getName()).append(" ");
                }
            }
        }

        doc.setSearchText(searchTextBuilder.toString());

        return doc;
    }
}