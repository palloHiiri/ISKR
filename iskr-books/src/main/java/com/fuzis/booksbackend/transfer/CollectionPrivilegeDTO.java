package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionPrivilegeDTO {
    private Integer cvpId;
    private Integer collectionId;
    private Integer userId;
    private String username;
    private String nickname;
    private String status; 
}