package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularUserDTO {
    private Integer userId;
    private String username;
    private String nickname;
    private String email;
    private String status;
    private Long subscribersCount;
    private ImageLinkDTO profileImage;
}