package com.revplay.musicplatform.analytics.service.impl;

import com.revplay.musicplatform.analytics.service.PlaybackAnalyticsService;
import com.revplay.musicplatform.analytics.service.UserStatisticsService;
import com.revplay.musicplatform.analytics.dto.response.ActiveUsersMetricsResponse;
import com.revplay.musicplatform.analytics.dto.response.ContentPerformanceResponse;
import com.revplay.musicplatform.analytics.dto.response.DashboardMetricsResponse;
import com.revplay.musicplatform.analytics.dto.response.GenrePlayCountResponse;
import com.revplay.musicplatform.analytics.dto.response.UserListeningStatsResponse;
import com.revplay.musicplatform.analytics.dto.response.TopArtistResponse;
import com.revplay.musicplatform.analytics.dto.response.TrendingContentResponse;
import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.enums.TimePeriod;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.playback.util.PlaybackValidationUtil;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaybackAnalyticsServiceImpl implements PlaybackAnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybackAnalyticsServiceImpl.class);

    private static final String TRENDING_SONGS_SQL = """
            SELECT 'song' AS type, s.song_id AS content_id, s.title, COUNT(ph.play_id) AS play_count
            FROM play_history ph
            JOIN songs s ON s.song_id = ph.song_id
            WHERE ph.played_at >= ?
              AND s.is_active = true
            GROUP BY s.song_id, s.title
            ORDER BY play_count DESC
            LIMIT ?
            """;
    private static final String TRENDING_PODCAST_SQL = """
            SELECT 'podcast' AS type, pe.episode_id AS content_id, pe.title, COUNT(ph.play_id) AS play_count
            FROM play_history ph
            JOIN podcast_episodes pe ON pe.episode_id = ph.episode_id
            WHERE ph.played_at >= ?
            GROUP BY pe.episode_id, pe.title
            ORDER BY play_count DESC
            LIMIT ?
            """;
    private static final String TOP_SONGS_SQL = """
            SELECT 'song' AS type, s.song_id AS content_id, s.title, COUNT(ph.play_id) AS play_count
            FROM play_history ph
            JOIN songs s ON s.song_id = ph.song_id
            WHERE s.is_active = true
            GROUP BY s.song_id, s.title
            ORDER BY play_count DESC
            LIMIT ?
            """;
    private static final String TOP_PODCASTS_SQL = """
            SELECT 'podcast' AS type, pe.episode_id AS content_id, pe.title, COUNT(ph.play_id) AS play_count
            FROM play_history ph
            JOIN podcast_episodes pe ON pe.episode_id = ph.episode_id
            GROUP BY pe.episode_id, pe.title
            ORDER BY play_count DESC
            LIMIT ?
            """;
    private static final String TOP_ARTISTS_SQL = """
            SELECT a.artist_id, a.display_name, COUNT(ph.play_id) AS play_count
            FROM play_history ph
            JOIN songs s ON s.song_id = ph.song_id
            JOIN artists a ON a.artist_id = s.artist_id
            GROUP BY a.artist_id, a.display_name
            ORDER BY play_count DESC
            LIMIT ?
            """;
    private static final String TOP_GENRES_SQL = """
            SELECT g.name AS genre, COUNT(ph.play_id) AS play_count
            FROM play_history ph
            JOIN songs s ON s.song_id = ph.song_id
            JOIN song_genres sg ON sg.song_id = s.song_id
            JOIN genres g ON g.genre_id = sg.genre_id
            WHERE ph.user_id = ?
            GROUP BY g.name
            ORDER BY play_count DESC
            LIMIT 5
            """;
    private static final String PEAK_HOUR_SQL = """
            SELECT HOUR(played_at) AS peak_hour
            FROM play_history
            WHERE user_id = ?
            GROUP BY HOUR(played_at)
            ORDER BY COUNT(*) DESC
            LIMIT 1
            """;
    private static final String TOTAL_PLAYS_SQL = "SELECT COUNT(*) FROM play_history";
    private static final String PLAYS_LAST_24H_SQL = "SELECT COUNT(*) FROM play_history WHERE played_at >= ?";
    private static final String ACTIVE_USERS_24H_SQL =
            "SELECT COUNT(DISTINCT user_id) FROM play_history WHERE played_at >= ?";
    private static final String ACTIVE_USERS_7D_SQL =
            "SELECT COUNT(DISTINCT user_id) FROM play_history WHERE played_at >= ?";
    private static final String ACTIVE_USERS_30D_SQL =
            "SELECT COUNT(DISTINCT user_id) FROM play_history WHERE played_at >= ?";

    private final JdbcTemplate jdbcTemplate;
    private final UserStatisticsService userStatisticsService;

    public PlaybackAnalyticsServiceImpl(JdbcTemplate jdbcTemplate, UserStatisticsService userStatisticsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userStatisticsService = userStatisticsService;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics.trending", key = "#type + ':' + #period + ':' + #limit")
    public List<TrendingContentResponse> trending(String type, TimePeriod period, int limit) {
        LOGGER.info("Fetching trending analytics: type={}, period={}, limit={}", type, period, limit);
        Instant since = period.sinceNow();
        String normalized = type == null ? "song" : type.toLowerCase();
        PlaybackValidationUtil.requireLimitInRange(limit, 1, 100);
        return queryTrendingByType(normalized, since, limit);
    }

    private List<TrendingContentResponse> queryTrendingByType(String normalized, Instant since, int limit) {
        try {
            if ("song".equals(normalized)) {
                return jdbcTemplate.query(TRENDING_SONGS_SQL, (rs, rowNum) -> new TrendingContentResponse(
                        rs.getString("type"),
                        rs.getLong("content_id"),
                        rs.getString("title"),
                        rs.getLong("play_count")
                ), since, limit);
            }
            if ("podcast".equals(normalized)) {
                return jdbcTemplate.query(TRENDING_PODCAST_SQL, (rs, rowNum) -> new TrendingContentResponse(
                        rs.getString("type"),
                        rs.getLong("content_id"),
                        rs.getString("title"),
                        rs.getLong("play_count")
                ), since, limit);
            }
        } catch (DataAccessException ex) {
            LOGGER.error("Trending analytics query failed for type={}", normalized, ex);
            return Collections.emptyList();
        }
        throw new PlaybackValidationException("type must be 'song' or 'podcast'");
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics.topArtists", key = "#limit")
    public List<TopArtistResponse> topArtists(int limit) {
        LOGGER.info("Fetching top artists analytics: limit={}", limit);
        PlaybackValidationUtil.requireLimitInRange(limit, 1, 100);
        try {
            return jdbcTemplate.query(TOP_ARTISTS_SQL, (rs, rowNum) -> new TopArtistResponse(
                    rs.getLong("artist_id"),
                    rs.getString("display_name"),
                    rs.getLong("play_count")
            ), limit);
        } catch (DataAccessException ex) {
            LOGGER.error("Top artists analytics query failed", ex);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<TrendingContentResponse> topContent(String type, int limit) {
        LOGGER.info("Fetching top content analytics: type={}, limit={}", type, limit);
        String normalized = type == null ? "song" : type.toLowerCase();
        PlaybackValidationUtil.requireLimitInRange(limit, 1, 100);
        try {
            if ("song".equals(normalized)) {
                return jdbcTemplate.query(TOP_SONGS_SQL, (rs, rowNum) -> new TrendingContentResponse(
                        rs.getString("type"),
                        rs.getLong("content_id"),
                        rs.getString("title"),
                        rs.getLong("play_count")
                ), limit);
            }
            if ("podcast".equals(normalized)) {
                return jdbcTemplate.query(TOP_PODCASTS_SQL, (rs, rowNum) -> new TrendingContentResponse(
                        rs.getString("type"),
                        rs.getLong("content_id"),
                        rs.getString("title"),
                        rs.getLong("play_count")
                ), limit);
            }
        } catch (DataAccessException ex) {
            LOGGER.error("Top content analytics query failed for type={}", normalized, ex);
            return Collections.emptyList();
        }
        throw new PlaybackValidationException("type must be 'song' or 'podcast'");
    }

    @Transactional
    public UserListeningStatsResponse userStats(Long userId) {
        LOGGER.info("Fetching user listening stats for userId={}", userId);
        try {
            UserStatisticsResponse baseStats = userStatisticsService.refreshAndGet(userId);
            List<GenrePlayCountResponse> topGenres = jdbcTemplate.query(TOP_GENRES_SQL, (rs, rowNum) -> new GenrePlayCountResponse(
                    rs.getString("genre"),
                    rs.getLong("play_count")
            ), userId);
            Integer peakHour = jdbcTemplate.query(
                    PEAK_HOUR_SQL,
                    rs -> rs.next() ? rs.getInt("peak_hour") : null,
                    userId
            );
            return new UserListeningStatsResponse(baseStats, topGenres, peakHour);
        } catch (DataAccessException ex) {
            LOGGER.error("User listening stats query failed for userId={}", userId, ex);
            UserStatisticsResponse baseStats = userStatisticsService.getByUserId(userId);
            return new UserListeningStatsResponse(baseStats, Collections.emptyList(), null);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "analytics.dashboard", key = "'metrics'")
    public DashboardMetricsResponse dashboardMetrics() {
        LOGGER.info("Fetching dashboard metrics");
        Instant now = Instant.now();
        try {
            Long totalPlatformPlays = nvl(jdbcTemplate.queryForObject(TOTAL_PLAYS_SQL, Long.class));
            Long playsLast24Hours = nvl(jdbcTemplate.queryForObject(PLAYS_LAST_24H_SQL, Long.class, now.minusSeconds(86400)));
            ActiveUsersMetricsResponse activeUsers = new ActiveUsersMetricsResponse(
                    nvl(jdbcTemplate.queryForObject(ACTIVE_USERS_24H_SQL, Long.class, now.minusSeconds(86400))),
                    nvl(jdbcTemplate.queryForObject(ACTIVE_USERS_7D_SQL, Long.class, now.minusSeconds(86400 * 7L))),
                    nvl(jdbcTemplate.queryForObject(ACTIVE_USERS_30D_SQL, Long.class, now.minusSeconds(86400 * 30L)))
            );

            TrendingContentResponse topSong = queryTrendingByType("song", TimePeriod.MONTHLY.sinceNow(), 1).stream().findFirst()
                    .orElse(new TrendingContentResponse("song", null, null, 0L));
            TrendingContentResponse topPodcast = queryTrendingByType("podcast", TimePeriod.MONTHLY.sinceNow(), 1).stream().findFirst()
                    .orElse(new TrendingContentResponse("podcast", null, null, 0L));
            ContentPerformanceResponse performance = new ContentPerformanceResponse(topSong, topPodcast);

            return new DashboardMetricsResponse(totalPlatformPlays, playsLast24Hours, activeUsers, performance);
        } catch (DataAccessException ex) {
            LOGGER.error("Dashboard metrics query failed", ex);
            ActiveUsersMetricsResponse activeUsers = new ActiveUsersMetricsResponse(0L, 0L, 0L);
            ContentPerformanceResponse performance = new ContentPerformanceResponse(
                    new TrendingContentResponse("song", null, null, 0L),
                    new TrendingContentResponse("podcast", null, null, 0L)
            );
            return new DashboardMetricsResponse(0L, 0L, activeUsers, performance);
        }
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }
}





