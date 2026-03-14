package com.revplay.musicplatform.artist.repository;

import java.util.Optional;

import com.revplay.musicplatform.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
    Optional<Artist> findByUserId(Long userId);
}
