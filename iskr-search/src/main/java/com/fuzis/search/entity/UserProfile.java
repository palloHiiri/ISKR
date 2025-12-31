package com.fuzis.search.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

// UserProfile.java
@Entity
@Table(name = "USER_PROFILES", schema = "ACCOUNTS")
@Data
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "up_id")
    private Integer upId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne
    @JoinColumn(name = "user_imgl_id")
    private ImageLinks userImglId;

    @Column(name = "nickname", nullable = false, length = 255)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 512)
    private String email;
}
