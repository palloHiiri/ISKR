package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {
    private Integer authorId;
    private String name;
    private String realName;
}