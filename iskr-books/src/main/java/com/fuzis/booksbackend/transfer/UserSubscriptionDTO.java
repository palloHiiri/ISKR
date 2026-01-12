package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private ImageLinkDTO profileImage;
    private Long subscribersCount; 
}