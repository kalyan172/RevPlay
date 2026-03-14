package com.revplay.musicplatform.analytics.service.impl;

import com.revplay.musicplatform.analytics.service.UserStatisticsService;
import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.entity.UserStatistics;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.analytics.mapper.UserStatisticsMapper;
import com.revplay.musicplatform.analytics.repository.UserStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserStatisticsServiceImpl.class);
    private static final String USER_EXISTS_SQL = "SELECT COUNT(1) FROM users WHERE user_id = ?";
    private static final String PLAYLISTS_SQL = "SELECT COUNT(*) FROM playlists WHERE user_id = ?";
    private static final String FAVORITES_SQL = "SELECT COUNT(*) FROM user_likes WHERE user_id = ?";
    private static final String LISTENING_TIME_SQL =
            "SELECT COALESCE(SUM(play_duration_seconds), 0) FROM play_history WHERE user_id = ?";
    private static final String SONGS_PLAYED_SQL =
            "SELECT COUNT(*) FROM play_history WHERE user_id = ? AND song_id IS NOT NULL";
    private static final String UPSERT_SQL = """
            INSERT INTO user_statistics (
              user_id,
              total_playlists,
              total_favorite_songs,
              total_listening_time_seconds,
              total_songs_played,
              last_updated
            ) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
              total_playlists = VALUES(total_playlists),
              total_favorite_songs = VALUES(total_favorite_songs),
              total_listening_time_seconds = VALUES(total_listening_time_seconds),
              total_songs_played = VALUES(total_songs_played),
              last_updated = CURRENT_TIMESTAMP
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserStatisticsRepository repository;
    private final UserStatisticsMapper userStatisticsMapper;

    public UserStatisticsServiceImpl(
            JdbcTemplate jdbcTemplate,
            UserStatisticsRepository repository,
            UserStatisticsMapper userStatisticsMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
        this.userStatisticsMapper = userStatisticsMapper;
    }

    @Transactional
    public UserStatisticsResponse getByUserId(Long userId) {
        LOGGER.info("Fetching user statistics for userId={}", userId);
        UserStatistics stats = repository.findByUserId(userId)
                .orElseGet(() -> refreshForUser(userId));
        return userStatisticsMapper.toDto(stats);
    }

    @Transactional
    public UserStatisticsResponse refreshAndGet(Long userId) {
        LOGGER.info("Refreshing user statistics for userId={}", userId);
        return userStatisticsMapper.toDto(refreshForUser(userId));
    }

    private UserStatistics refreshForUser(Long userId) {
        LOGGER.debug("Calculating user statistics aggregates for userId={}", userId);
        requireUser(userId);

        long totalPlaylists = nvl(jdbcTemplate.queryForObject(PLAYLISTS_SQL, Long.class, userId));
        long totalFavoriteSongs = nvl(jdbcTemplate.queryForObject(FAVORITES_SQL, Long.class, userId));
        long totalListeningTime = nvl(jdbcTemplate.queryForObject(LISTENING_TIME_SQL, Long.class, userId));
        long totalSongsPlayed = nvl(jdbcTemplate.queryForObject(SONGS_PLAYED_SQL, Long.class, userId));

        jdbcTemplate.update(
                UPSERT_SQL,
                userId,
                totalPlaylists,
                totalFavoriteSongs,
                totalListeningTime,
                totalSongsPlayed
        );

        return repository.findByUserId(userId)
                .orElseGet(() -> buildSnapshot(
                        userId,
                        totalPlaylists,
                        totalFavoriteSongs,
                        totalListeningTime,
                        totalSongsPlayed
                ));
    }

    private void requireUser(Long userId) {
        Long count = jdbcTemplate.queryForObject(USER_EXISTS_SQL, Long.class, userId);
        if (nvl(count) == 0L) {
            LOGGER.warn("User not found for userId={}", userId);
            throw new PlaybackNotFoundException("User " + userId + " does not exist");
        }
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private UserStatistics buildSnapshot(
            Long userId,
            long totalPlaylists,
            long totalFavoriteSongs,
            long totalListeningTime,
            long totalSongsPlayed
    ) {
        UserStatistics snapshot = new UserStatistics();
        snapshot.setUserId(userId);
        snapshot.setTotalPlaylists(totalPlaylists);
        snapshot.setTotalFavoriteSongs(totalFavoriteSongs);
        snapshot.setTotalListeningTimeSeconds(totalListeningTime);
        snapshot.setTotalSongsPlayed(totalSongsPlayed);
        snapshot.setLastUpdated(Instant.now());
        return snapshot;
    }
}






