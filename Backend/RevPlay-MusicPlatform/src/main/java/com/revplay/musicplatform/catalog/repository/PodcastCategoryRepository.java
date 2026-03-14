package com.revplay.musicplatform.catalog.repository;



import com.revplay.musicplatform.catalog.entity.PodcastCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastCategoryRepository extends JpaRepository<PodcastCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
}

