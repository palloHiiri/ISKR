package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Integer userId;
    private String username;
    private LocalDateTime registeredDate;
    private String nickname;
    private ImageLinkDTO profileImage;
}
