package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDTO {
    private Integer userId;
    private String username;
    private LocalDateTime registeredDate;
    private String nickname;
    private String email;
    private String profileDescription;
    private LocalDateTime birthDate;
    private Boolean emailVerified;
    private String status;
    private ImageLinkDTO profileImage;
    private Long subscribersCount;
    private Long subscriptionsCount;
    private Long collectionsCount; 
}