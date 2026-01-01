package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionDetailDTO {
    private Integer collectionId;
    private String title;
    private String description;
    private String confidentiality;
    private String collectionType;
    private ImageLinkDTO photoLink;
    private Integer ownerId;
    private String ownerNickname;
    private Long booksCount;
    private Long likesCount;
    private Boolean canView;
}