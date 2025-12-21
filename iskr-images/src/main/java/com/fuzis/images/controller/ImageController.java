package com.fuzis.images.controller;

import com.fuzis.images.entity.ImageLink;
import com.fuzis.images.service.ImageService;
import com.fuzis.images.transfer.ChangeDTO;
import com.fuzis.images.util.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageService imageService;
    private final HttpUtil httpUtil;

    @Autowired
    public ImageController(ImageService imageService, HttpUtil httpUtil) {
        this.imageService = imageService;
        this.httpUtil = httpUtil;
    }

    @PostMapping("/upload")
    public ResponseEntity<ChangeDTO<ImageLink>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("X-User-Id") Integer uploaderId) {
        return httpUtil.handleServiceResponse(imageService.uploadImage(file, uploaderId));
    }
}