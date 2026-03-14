package com.revplay.musicplatform.catalog.repository;



import com.revplay.musicplatform.catalog.entity.PodcastEpisode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastEpisodeRepository extends JpaRepository<PodcastEpisode, Long> {
    Page<PodcastEpisode> findByPodcastId(Long podcastId, Pageable pageable);
}

