package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.catalog.entity.Album;
import com.revplay.musicplatform.catalog.repository.AlbumRepository;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.catalog.service.ContentValidationService;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContentValidationServiceImpl implements ContentValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentValidationServiceImpl.class);
    private static final int MAX_SONG_DURATION_SECONDS = 60 * 60;
    private static final int MAX_PODCAST_EPISODE_DURATION_SECONDS = 3 * 60 * 60;

    private final AlbumRepository albumRepository;
    private final SongRepository songRepository;

    public ContentValidationServiceImpl(AlbumRepository albumRepository, SongRepository songRepository) {
        this.albumRepository = albumRepository;
        this.songRepository = songRepository;
    }

    public void validateSongDuration(Integer durationSeconds) {
        validateDuration(durationSeconds, MAX_SONG_DURATION_SECONDS, "song");
    }

    public void validatePodcastEpisodeDuration(Integer durationSeconds) {
        validateDuration(durationSeconds, MAX_PODCAST_EPISODE_DURATION_SECONDS, "podcast episode");
    }

    public void validateAlbumBelongsToArtist(Long albumId, Long artistId) {
        LOGGER.debug("Validating album ownership: albumId={}, artistId={}", albumId, artistId);
        if (albumId == null) {
            return;
        }
        Album album = albumRepository.findById(albumId)
            .orElseThrow(() -> new ResourceNotFoundException("Album not found"));
        if (!album.getArtistId().equals(artistId)) {
            throw new BadRequestException("Album does not belong to the song artist");
        }
    }

    public void validateUniqueSongTitleWithinAlbum(Long albumId, String title) {
        if (albumId == null || title == null || title.isBlank()) {
            return;
        }
        if (songRepository.existsByAlbumIdAndTitleIgnoreCaseAndIsActiveTrue(albumId, title.trim())) {
            throw new BadRequestException("Duplicate song title exists in album");
        }
    }

    public void validateUniqueSongTitleWithinAlbumForUpdate(Long albumId, String title, Long songId) {
        if (albumId == null || title == null || title.isBlank() || songId == null) {
            return;
        }
        if (songRepository.existsByAlbumIdAndTitleIgnoreCaseAndIsActiveTrueAndSongIdNot(albumId, title.trim(), songId)) {
            throw new BadRequestException("Duplicate song title exists in album");
        }
    }

    private void validateDuration(Integer durationSeconds, int maxSeconds, String contentLabel) {
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new BadRequestException("Invalid " + contentLabel + " duration");
        }
        if (durationSeconds > maxSeconds) {
            throw new BadRequestException(contentLabel + " duration exceeds allowed limit");
        }
    }
}


