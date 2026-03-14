package com.revplay.musicplatform.download.service;

import org.springframework.core.io.Resource;

public interface DownloadService {

    Resource downloadSong(Long userId, Long songId);

    boolean isDownloaded(Long userId, Long songId);

    String getDownloadFileName(Long songId);
}

