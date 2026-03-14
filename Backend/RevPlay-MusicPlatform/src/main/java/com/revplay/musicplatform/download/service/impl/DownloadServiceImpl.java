package com.revplay.musicplatform.download.service.impl;

import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.download.entity.SongDownload;
import com.revplay.musicplatform.download.repository.SongDownloadRepository;
import com.revplay.musicplatform.download.service.DownloadService;
import com.revplay.musicplatform.download.service.SongFileResolver;
import com.revplay.musicplatform.exception.AccessDeniedException;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import com.revplay.musicplatform.premium.service.SubscriptionService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DownloadServiceImpl implements DownloadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadServiceImpl.class);

    private final SubscriptionService subscriptionService;
    private final SongRepository songRepository;
    private final SongFileResolver songFileResolver;
    private final SongDownloadRepository songDownloadRepository;

    public DownloadServiceImpl(
            SubscriptionService subscriptionService,
            SongRepository songRepository,
            SongFileResolver songFileResolver,
            SongDownloadRepository songDownloadRepository
    ) {
        this.subscriptionService = subscriptionService;
        this.songRepository = songRepository;
        this.songFileResolver = songFileResolver;
        this.songDownloadRepository = songDownloadRepository;
    }

    @Override
    @Transactional
    public Resource downloadSong(Long userId, Long songId) {
        validateIds(userId, songId);

        if (!subscriptionService.isUserPremium(userId)) {
            throw new AccessDeniedException("Premium subscription required to download songs");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song", songId));
        Resource resource = songFileResolver.loadSongResource(song.getFileUrl());

        if (!songDownloadRepository.existsByUserIdAndSongId(userId, songId)) {
            SongDownload songDownload = new SongDownload();
            songDownload.setUserId(userId);
            songDownload.setSongId(songId);
            songDownload.setDownloadedAt(LocalDateTime.now());
            songDownloadRepository.save(songDownload);
            LOGGER.info("Song downloaded: userId={}, songId={}", userId, songId);
        }

        return resource;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDownloaded(Long userId, Long songId) {
        validateIds(userId, songId);
        return songDownloadRepository.existsByUserIdAndSongId(userId, songId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDownloadFileName(Long songId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song", songId));
        String title = (song.getTitle() == null || song.getTitle().isBlank()) ? ("song-" + songId) : song.getTitle().trim();
        String safeTitle = title.replaceAll("[^a-zA-Z0-9\\-_ ]", "").replace(' ', '-');
        return safeTitle + ".mp3";
    }

    private void validateIds(Long userId, Long songId) {
        if (userId == null || userId <= 0) {
            throw new BadRequestException("userId is required");
        }
        if (songId == null || songId <= 0) {
            throw new BadRequestException("songId is required");
        }
    }
}

