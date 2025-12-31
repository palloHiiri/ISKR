package com.fuzis.search.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

// User.java - обновляем с учетом профиля
@Entity
@Table(name = "USERS", schema = "ACCOUNTS")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserProfile profile;

    @Transient
    private Long subscribersCount;
}
