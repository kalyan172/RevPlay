package com.revplay.musicplatform.systemplaylist.repository;

import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPlaylistRepository extends JpaRepository<SystemPlaylist, Long> {

    List<SystemPlaylist> findByIsActiveTrueAndDeletedAtIsNull();

    Optional<SystemPlaylist> findBySlugAndDeletedAtIsNull(String slug);
}

