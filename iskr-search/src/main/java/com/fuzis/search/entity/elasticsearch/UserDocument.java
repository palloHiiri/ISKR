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

    public static UserDocument fromEntity(User user) {
        UserDocument doc = new UserDocument();
        doc.setId("user_" + user.getUser_id());
        doc.setType("user");
        doc.setIndexedAt(LocalDate.now());
        doc.setUsername(user.getUsername());
        doc.setNickname(user.getProfile().getNickname());
        doc.setEmail(user.getProfile().getEmail());

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getUser_id());
        data.put("username", user.getUsername());
        data.put("nickname", user.getProfile().getNickname());
        data.put("email", user.getProfile().getEmail());
        doc.setData(data);

        doc.setSearchText(
                (user.getUsername() != null ? user.getUsername() + " " : "") +
                (user.getProfile().getNickname() != null ? user.getProfile().getNickname() + " ": "") +
                (user.getProfile().getEmail() != null ? user.getProfile().getEmail() : "")
        );

        return doc;
    }
}
