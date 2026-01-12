package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularCollectionDTO {
    private Integer collectionId;
    private String title;
    private String description;
    private String collectionType;
    private Integer ownerId;
    private String ownerNickname;
    private Long likesCount;
    private Integer bookCount; 
    private ImageLinkDTO photoLink;
}