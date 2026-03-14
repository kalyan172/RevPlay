package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.catalog.service.DiscoveryFeedService;
import com.revplay.musicplatform.catalog.service.BrowseService;
import com.revplay.musicplatform.catalog.dto.response.DiscoverWeeklyResponse;
import com.revplay.musicplatform.catalog.dto.response.DiscoveryFeedResponse;
import com.revplay.musicplatform.catalog.dto.response.DiscoveryRecommendationItemResponse;
import com.revplay.musicplatform.catalog.dto.response.NewReleaseItemResponse;
import com.revplay.musicplatform.catalog.dto.response.PopularPodcastItemResponse;
import com.revplay.musicplatform.catalog.dto.response.TopArtistItemResponse;
import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import com.revplay.musicplatform.catalog.util.DiscoveryValidationUtil;
import com.revplay.musicplatform.analytics.dto.response.ForYouRecommendationsResponse;
import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;
import com.revplay.musicplatform.analytics.service.RecommendationService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiscoveryFeedServiceImpl implements DiscoveryFeedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryFeedService.class);
    private static final String DISCOVER_WEEKLY_RECENT_SQL = """
            SELECT
                s.song_id,
                s.title,
                a.artist_id,
                a.display_name AS artist_name,
                (COUNT(DISTINCT ph_all.play_id) * 5
                 + GREATEST(0, 365 - DATEDIFF(CURRENT_DATE, s.release_date))) AS score
            FROM songs s
            JOIN artists a ON a.artist_id = s.artist_id
            JOIN song_genres sg ON sg.song_id = s.song_id
            LEFT JOIN play_history ph_all ON ph_all.song_id = s.song_id
            WHERE sg.genre_id IN (
                SELECT sg2.genre_id
                FROM play_history ph2
                JOIN song_genres sg2 ON sg2.song_id = ph2.song_id
                WHERE ph2.user_id = ?
                  AND ph2.played_at >= (CURRENT_TIMESTAMP - INTERVAL 90 DAY)
                  AND ph2.song_id IS NOT NULL
                GROUP BY sg2.genre_id
                ORDER BY COUNT(*) DESC
                LIMIT 5
            )
              AND s.song_id NOT IN (
                SELECT DISTINCT ph3.song_id
                FROM play_history ph3
                WHERE ph3.user_id = ?
                  AND ph3.song_id IS NOT NULL
            )
              AND s.is_active = true
              AND s.release_date >= (CURRENT_DATE - INTERVAL 365 DAY)
            GROUP BY s.song_id, s.title, a.artist_id, a.display_name, s.release_date
            ORDER BY score DESC, RAND(?)
            LIMIT ?
            """;

    private final RecommendationService recommendationService;
    private final BrowseService browseService;
    private final JdbcTemplate jdbcTemplate;

    public DiscoveryFeedServiceImpl(
            RecommendationService recommendationService,
            BrowseService browseService,
            JdbcTemplate jdbcTemplate
    ) {
        this.recommendationService = recommendationService;
        this.browseService = browseService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(cacheNames = "discover.weekly", key = "#userId + ':' + #limit")
    public DiscoverWeeklyResponse discoverWeekly(Long userId, int limit) {
        LOGGER.info("Building discover weekly feed for userId={}, limit={}", userId, limit);
        DiscoveryValidationUtil.requirePositiveId(userId, "userId");
        if (limit < 1 || limit > 100) {
            throw new DiscoveryValidationException("limit must be between 1 and 100");
        }

        List<DiscoveryRecommendationItemResponse> items = safeDiscoverWeekly(userId, limit);
        return new DiscoverWeeklyResponse(userId, items);
    }

    @Cacheable(cacheNames = "discover.feed", key = "#userId + ':' + #sectionLimit")
    public DiscoveryFeedResponse homeFeed(Long userId, int sectionLimit) {
        LOGGER.info("Building home discovery feed for userId={}, sectionLimit={}", userId, sectionLimit);
        DiscoveryValidationUtil.requirePositiveId(userId, "userId");
        if (sectionLimit < 1 || sectionLimit > 50) {
            throw new DiscoveryValidationException("sectionLimit must be between 1 and 50");
        }

        List<NewReleaseItemResponse> newReleases = browseService.newReleases(0, sectionLimit, "DESC").getContent();
        List<TopArtistItemResponse> topArtists = browseService.topArtists(0, sectionLimit).getContent();
        List<PopularPodcastItemResponse> popularPodcasts = browseService.popularPodcasts(0, sectionLimit).getContent();
        List<DiscoveryRecommendationItemResponse> discoverWeekly = safeDiscoverWeekly(userId, sectionLimit);

        return new DiscoveryFeedResponse(
                userId,
                newReleases,
                topArtists,
                popularPodcasts,
                discoverWeekly
        );
    }

    private List<DiscoveryRecommendationItemResponse> safeDiscoverWeekly(Long userId, int limit) {
        List<DiscoveryRecommendationItemResponse> recentRecommendations = discoverWeeklyWithRecentSignals(userId, limit);
        if (!recentRecommendations.isEmpty()) {
            return recentRecommendations;
        }
        try {
            ForYouRecommendationsResponse forYouRecommendations = recommendationService.forUser(userId, limit);
            List<DiscoveryRecommendationItemResponse> fallback = forYouRecommendations.youMightLike().stream()
                    .map(this::mapRecommendation)
                    .toList();
            if (!fallback.isEmpty()) {
                return fallback;
            }
            return forYouRecommendations.popularWithSimilarUsers().stream()
                    .map(this::mapRecommendation)
                    .toList();
        } catch (RuntimeException ex) {
            LOGGER.warn("Unable to load discover weekly recommendations for userId={}. Returning empty list.", userId);
            return Collections.emptyList();
        }
    }

    private List<DiscoveryRecommendationItemResponse> discoverWeeklyWithRecentSignals(Long userId, int limit) {
        if (!hasTables("songs", "artists", "song_genres", "play_history")) {
            return List.of();
        }
        try {
            long dailySeed = userId + LocalDate.now().toEpochDay();
            return jdbcTemplate.query(
                    DISCOVER_WEEKLY_RECENT_SQL,
                    (rs, rowNum) -> new DiscoveryRecommendationItemResponse(
                            rs.getLong("song_id"),
                            rs.getString("title"),
                            rs.getLong("artist_id"),
                            rs.getString("artist_name"),
                            rs.getLong("score")
                    ),
                    userId,
                    userId,
                    dailySeed,
                    limit
            );
        } catch (DataAccessException ex) {
            LOGGER.warn("Recent-signal discover query failed for userId={}. Falling back.", userId);
            return List.of();
        }
    }

    private DiscoveryRecommendationItemResponse mapRecommendation(SongRecommendationResponse dto) {
        return new DiscoveryRecommendationItemResponse(
                dto.songId(),
                dto.title(),
                dto.artistId(),
                dto.artistName(),
                dto.score()
        );
    }

    private boolean hasTables(String... tableNames) {
        for (String tableName : tableNames) {
            Long count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(1)
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = ?
                    """,
                    Long.class,
                    tableName
            );
            if (count == null || count == 0) {
                return false;
            }
        }
        return true;
    }
}



