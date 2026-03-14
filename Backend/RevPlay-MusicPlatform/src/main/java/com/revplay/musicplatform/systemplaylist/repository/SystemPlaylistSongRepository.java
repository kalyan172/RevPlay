package com.revplay.musicplatform.systemplaylist.repository;

import com.revplay.musicplatform.systemplaylist.entity.SystemPlaylistSong;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPlaylistSongRepository extends JpaRepository<SystemPlaylistSong, Long> {

    List<SystemPlaylistSong> findBySystemPlaylistIdAndDeletedAtIsNullOrderByPositionAsc(Long playlistId);

    boolean existsBySystemPlaylistIdAndSongIdAndDeletedAtIsNull(Long playlistId, Long songId);
}
