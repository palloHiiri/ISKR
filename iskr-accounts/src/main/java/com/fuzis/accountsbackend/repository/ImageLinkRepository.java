package com.fuzis.accountsbackend.repository;

import com.fuzis.accountsbackend.entity.ImageLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageLinkRepository extends JpaRepository<ImageLink, Integer> {
}
