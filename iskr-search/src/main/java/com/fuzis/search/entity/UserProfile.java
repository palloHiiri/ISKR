package com.fuzis.search.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fuzis.search.entity.enumerate.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name="user_profiles", schema = "accounts")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer up_id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer user_imgl_id;

    private String nickname;

    private String email;

    private Boolean email_verified;

    private String profile_description;

    private ZonedDateTime birth_date;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    public UserProfile(User user, String nickname, String email) {
        this.user = user;
        this.nickname = nickname;
        this.email = email;
        this.email_verified = false;
        this.profile_description = "";
        this.status = UserStatus.notBanned;
    }
}
