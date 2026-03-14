package com.revplay.musicplatform.playback.service.impl;

import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;
import com.revplay.musicplatform.analytics.service.RecommendationService;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.repository.SongRepository;
import com.revplay.musicplatform.playback.dto.response.QueueItemResponse;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.playback.service.AutoplayService;
import com.revplay.musicplatform.playback.service.QueueService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutoplayServiceImpl implements AutoplayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoplayServiceImpl.class);

    private final QueueService queueService;
    private final RecommendationService recommendationService;
    private final SongRepository songRepository;

    public AutoplayServiceImpl(
            QueueService queueService,
            RecommendationService recommendationService,
            SongRepository songRepository
    ) {
        this.queueService = queueService;
        this.recommendationService = recommendationService;
        this.songRepository = songRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Song getNextSong(Long userId, Long currentSongId) {
        LOGGER.info("Resolving autoplay next song for userId={}, currentSongId={}", userId, currentSongId);
        if (userId == null || currentSongId == null) {
            throw new PlaybackValidationException("userId and currentSongId are required");
        }

        Song nextFromQueue = tryQueueNextSong(userId, currentSongId);
        if (nextFromQueue != null) {
            return nextFromQueue;
        }

        return fallbackToRecommendations(currentSongId);
    }

    private Song tryQueueNextSong(Long userId, Long currentSongId) {
        List<QueueItemResponse> queue = queueService.getQueue(userId);
        if (queue.isEmpty()) {
            LOGGER.info("Queue is empty for userId={}; falling back to recommendations", userId);
            return null;
        }

        QueueItemResponse currentQueueItem = queue.stream()
                .filter(item -> currentSongId.equals(item.songId()))
                .findFirst()
                .orElse(null);
        if (currentQueueItem == null) {
            LOGGER.info("Current songId={} not found in queue for userId={}; falling back to recommendations", currentSongId, userId);
            return null;
        }

        QueueItemResponse nextQueueItem = queueService.next(userId, currentQueueItem.queueId());
        if (nextQueueItem.songId() == null) {
            LOGGER.info("Next queue item queueId={} is not a song; falling back to recommendations", nextQueueItem.queueId());
            return null;
        }
        return songRepository.findById(nextQueueItem.songId())
                .orElseThrow(() -> new PlaybackNotFoundException("Song " + nextQueueItem.songId() + " not found"));
    }

    private Song fallbackToRecommendations(Long currentSongId) {
        List<SongRecommendationResponse> recommendations = recommendationService.similarSongs(currentSongId, 1);
        if (recommendations.isEmpty()) {
            LOGGER.warn("No recommendations found for currentSongId={}", currentSongId);
            throw new PlaybackNotFoundException("No autoplay song available");
        }

        Long recommendedSongId = recommendations.get(0).songId();
        LOGGER.info("Using recommended songId={} for autoplay fallback", recommendedSongId);
        return songRepository.findById(recommendedSongId)
                .orElseThrow(() -> new PlaybackNotFoundException("Song " + recommendedSongId + " not found"));
    }

}
