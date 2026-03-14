package com.revplay.musicplatform.playback.service.impl;

import java.time.Instant;
import java.util.List;

import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.playback.dto.request.TrackPlayRequest;
import com.revplay.musicplatform.playback.dto.response.PlayHistoryResponse;
import com.revplay.musicplatform.playback.entity.PlayHistory;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.playback.mapper.PlayHistoryMapper;
import com.revplay.musicplatform.playback.repository.PlayHistoryRepository;
import com.revplay.musicplatform.playback.service.PlayHistoryService;
import com.revplay.musicplatform.playback.util.PlaybackValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayHistoryServiceImpl implements PlayHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayHistoryServiceImpl.class);
    private static final String USER_EXISTS_SQL = "SELECT COUNT(1) FROM users WHERE user_id = ?";
    private final JdbcTemplate jdbcTemplate;
    private final PlayHistoryRepository playHistoryRepository;
    private final PlayHistoryMapper playHistoryMapper;
    private final SongRepository songRepository;

    public PlayHistoryServiceImpl(
            JdbcTemplate jdbcTemplate,
            PlayHistoryRepository playHistoryRepository,
            PlayHistoryMapper playHistoryMapper,
            SongRepository songRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.playHistoryRepository = playHistoryRepository;
        this.playHistoryMapper = playHistoryMapper;
        this.songRepository = songRepository;
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                    "analytics.trending",
                    "analytics.dashboard",
                    "analytics.topArtists",
                    "artist.dashboard",
                    "artist.popularity"
            },
            allEntries = true
    )
    public void trackPlay(TrackPlayRequest request) {
        LOGGER.info("Tracking play for userId={}, songId={}, episodeId={}",
                request == null ? null : request.userId(),
                request == null ? null : request.songId(),
                request == null ? null : request.episodeId());
        validate(request);
        PlayHistory entity = new PlayHistory();
        entity.setUserId(request.userId());
        entity.setSongId(request.songId());
        entity.setEpisodeId(request.episodeId());
        Instant playedAt = request.playedAt() == null ? Instant.now() : request.playedAt();
        entity.setPlayedAt(playedAt);
        entity.setCompleted(request.completed() == null ? Boolean.FALSE : request.completed());
        entity.setPlayDurationSeconds(request.playDurationSeconds() == null ? 0 : request.playDurationSeconds());
        playHistoryRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<PlayHistoryResponse> getHistory(Long userId) {
        LOGGER.info("Fetching play history for userId={}", userId);
        requireUser(userId);
        return playHistoryRepository.findByUserIdOrderByPlayedAtDescPlayIdDesc(userId).stream()
                .filter(this::hasActiveSongOrNoSong)
                .map(playHistoryMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayHistoryResponse> recentlyPlayed(Long userId) {
        LOGGER.info("Fetching recently played content for userId={}", userId);
        requireUser(userId);
        return playHistoryRepository.findByUserIdOrderByPlayedAtDescPlayIdDesc(userId, PageRequest.of(0, 50)).stream()
                .filter(this::hasActiveSongOrNoSong)
                .map(playHistoryMapper::toDto)
                .toList();
    }

    @Transactional
    @CacheEvict(
            cacheNames = {
                    "analytics.trending",
                    "analytics.dashboard",
                    "analytics.topArtists",
                    "artist.dashboard",
                    "artist.popularity"
            },
            allEntries = true
    )
    public long clearHistory(Long userId) {
        LOGGER.info("Clearing play history for userId={}", userId);
        requireUser(userId);
        return playHistoryRepository.deleteByUserId(userId);
    }

    private void validate(TrackPlayRequest request) {
        if (request == null || request.userId() == null) {
            throw new PlaybackValidationException("userId is required");
        }
        requireUser(request.userId());
        PlaybackValidationUtil.requireExactlyOneContentId(request.songId(), request.episodeId());
    }

    private void requireUser(Long userId) {
        Long count = jdbcTemplate.queryForObject(USER_EXISTS_SQL, Long.class, userId);
        if ((count == null ? 0L : count) == 0L) {
            LOGGER.warn("User not found for userId={}", userId);
            throw new PlaybackNotFoundException("User " + userId + " does not exist");
        }
    }

    private boolean hasActiveSongOrNoSong(PlayHistory history) {
        return history.getSongId() == null || songRepository.existsBySongIdAndIsActiveTrue(history.getSongId());
    }

}





