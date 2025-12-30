package com.fuzis.booksbackend.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageDataDTO {
    private Integer imgdId;
    private String uuid;
    private Integer size;
    private String mimeType;
    private String extension;
}