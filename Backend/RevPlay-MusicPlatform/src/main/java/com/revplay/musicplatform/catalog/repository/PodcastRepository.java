package com.revplay.musicplatform.catalog.repository;



import com.revplay.musicplatform.catalog.entity.Podcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PodcastRepository extends JpaRepository<Podcast, Long> {
    Page<Podcast> findByArtistIdAndIsActiveTrue(Long artistId, Pageable pageable);
    long countByArtistIdAndIsActiveTrue(Long artistId);

    @Query("""
        select p from Podcast p
        left join PodcastEpisode e on e.podcastId = p.podcastId
        where p.isActive = true
        group by p
        order by count(e) desc
        """)
    Page<Podcast> findRecommended(Pageable pageable);
}
