package com.fuzis.images.repository;

import com.fuzis.images.entity.ImageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageDataRepository extends JpaRepository<ImageData, Integer> {
    ImageData findByUuid(String uuid);
}