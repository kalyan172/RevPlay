package com.revplay.musicplatform.download.repository;

import com.revplay.musicplatform.download.entity.SongDownload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongDownloadRepository extends JpaRepository<SongDownload, Long> {

    boolean existsByUserIdAndSongId(Long userId, Long songId);
}

