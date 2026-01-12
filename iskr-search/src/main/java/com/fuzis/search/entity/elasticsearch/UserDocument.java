package com.fuzis.search.entity.elasticsearch;

import com.fuzis.search.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class UserDocument extends BaseIndexDocument {
    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String username;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "russian"),
            otherFields = {
                    @InnerField(suffix = "en", type = FieldType.Text, analyzer = "english"),
                    @InnerField(suffix = "keyword", type = FieldType.Keyword)
            }
    )
    private String nickname;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Long)
    private Long subscribersCount;

    @Field(type = FieldType.Keyword)
    private String imageUuid;

    @Field(type = FieldType.Keyword)
    private String imageExtension;

    public static UserDocument fromEntity(User user) {
        UserDocument doc = new UserDocument();
        doc.setId("user_" + user.getUserId());
        doc.setType("user");
        doc.setIndexedAt(LocalDate.now());
        doc.setUsername(user.getUsername());

        if (user.getProfile() != null) {
            doc.setNickname(user.getProfile().getNickname());
            doc.setEmail(user.getProfile().getEmail());

            if (user.getProfile().getUserImglId() != null &&
                    user.getProfile().getUserImglId().getImageData() != null) {
                doc.setImageUuid(user.getProfile().getUserImglId().getImageData().getUuid());
                doc.setImageExtension(user.getProfile().getUserImglId().getImageData().getExtension());
            }
        }

        doc.setSubscribersCount(user.getSubscribersCount());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getUserId());
        data.put("username", user.getUsername());

        if (user.getProfile() != null) {
            data.put("nickname", user.getProfile().getNickname());
            data.put("email", user.getProfile().getEmail());
        }

        data.put("subscribersCount", user.getSubscribersCount());

        if (doc.getImageUuid() != null) {
            data.put("imageUuid", doc.getImageUuid());
            data.put("imageExtension", doc.getImageExtension());
        }

        doc.setData(data);

        StringBuilder searchTextBuilder = new StringBuilder();
        if (user.getUsername() != null) searchTextBuilder.append(user.getUsername()).append(" ");

        if (user.getProfile() != null) {
            if (user.getProfile().getNickname() != null) {
                searchTextBuilder.append(user.getProfile().getNickname()).append(" ");
            }
            if (user.getProfile().getEmail() != null) {
                searchTextBuilder.append(user.getProfile().getEmail()).append(" ");
            }
        }

        doc.setSearchText(searchTextBuilder.toString());

        return doc;
    }
}