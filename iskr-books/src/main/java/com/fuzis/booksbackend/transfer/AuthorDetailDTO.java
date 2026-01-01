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
public class AuthorDetailDTO {
    private Integer authorId;
    private String name;
    private LocalDateTime birthDate;
    private String description;
    private String realName;
}
