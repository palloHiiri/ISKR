package com.fuzis.images.repository;

import com.fuzis.images.entity.ImageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageLinkRepository extends JpaRepository<ImageLink, Integer> {
}