package com.revplay.musicplatform.catalog.repository;



import com.revplay.musicplatform.catalog.entity.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository extends JpaRepository<Album, Long> {
    Page<Album> findByArtistIdAndIsActiveTrue(Long artistId, Pageable pageable);
    long countByArtistIdAndIsActiveTrue(Long artistId);
}

