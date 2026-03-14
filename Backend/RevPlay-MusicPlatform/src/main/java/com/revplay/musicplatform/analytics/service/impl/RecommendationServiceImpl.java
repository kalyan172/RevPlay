package com.revplay.musicplatform.analytics.service.impl;

import com.revplay.musicplatform.analytics.service.RecommendationService;
import com.revplay.musicplatform.analytics.dto.response.ForYouRecommendationsResponse;
import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.playback.util.PlaybackValidationUtil;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    private static final String SIMILAR_SONGS_SQL = """
            SELECT
              s2.song_id,
              s2.title,
              a.artist_id,
              a.display_name AS artist_name,
              (COUNT(DISTINCT sg2.genre_id) * 100 + COUNT(DISTINCT ph.play_id)) AS score
            FROM songs s1
            JOIN song_genres sg1 ON sg1.song_id = s1.song_id
            JOIN song_genres sg2 ON sg2.genre_id = sg1.genre_id
            JOIN songs s2 ON s2.song_id = sg2.song_id AND s2.song_id <> s1.song_id
            JOIN artists a ON a.artist_id = s2.artist_id
            LEFT JOIN play_history ph ON ph.song_id = s2.song_id
            WHERE s1.song_id = ?
              AND s1.is_active = true
              AND s2.is_active = true
            GROUP BY s2.song_id, s2.title, a.artist_id, a.display_name
            ORDER BY score DESC, s2.song_id ASC
            LIMIT ?
            """;
    private static final String YOU_MIGHT_LIKE_SQL = """
            SELECT
              s.song_id,
              s.title,
              a.artist_id,
              a.display_name AS artist_name,
              (COUNT(DISTINCT ph_all.play_id) * 10 + COUNT(DISTINCT sg.genre_id)) AS score
            FROM songs s
            JOIN artists a ON a.artist_id = s.artist_id
            JOIN song_genres sg ON sg.song_id = s.song_id
            LEFT JOIN play_history ph_all ON ph_all.song_id = s.song_id
            JOIN (
              SELECT sg2.genre_id
              FROM play_history ph2
              JOIN song_genres sg2 ON sg2.song_id = ph2.song_id
              JOIN songs s2 ON s2.song_id = ph2.song_id
              WHERE ph2.user_id = ?
                AND s2.is_active = true
              GROUP BY sg2.genre_id
              ORDER BY COUNT(*) DESC
              LIMIT 5
            ) top_genres ON top_genres.genre_id = sg.genre_id
            AND s.song_id NOT IN (
              SELECT DISTINCT ph3.song_id FROM play_history ph3 WHERE ph3.user_id = ? AND ph3.song_id IS NOT NULL
            )
            WHERE s.is_active = true
            GROUP BY s.song_id, s.title, a.artist_id, a.display_name
            ORDER BY score DESC, s.song_id ASC
            LIMIT ?
            """;
    private static final String POPULAR_WITH_SIMILAR_USERS_SQL = """
            SELECT
              s.song_id,
              s.title,
              a.artist_id,
              a.display_name AS artist_name,
              COUNT(ph_candidate.play_id) AS score
            FROM (
              SELECT ph_other.user_id
              FROM play_history ph_self
              JOIN play_history ph_other ON ph_self.song_id = ph_other.song_id
              WHERE ph_self.user_id = ? AND ph_other.user_id <> ?
              GROUP BY ph_other.user_id
              ORDER BY COUNT(*) DESC
              LIMIT 20
            ) similar_users
            JOIN play_history ph_candidate ON ph_candidate.user_id = similar_users.user_id
            JOIN songs s ON s.song_id = ph_candidate.song_id
            JOIN artists a ON a.artist_id = s.artist_id
            WHERE ph_candidate.song_id NOT IN (
              SELECT DISTINCT ph_seen.song_id FROM play_history ph_seen WHERE ph_seen.user_id = ? AND ph_seen.song_id IS NOT NULL
            )
              AND s.is_active = true
            GROUP BY s.song_id, s.title, a.artist_id, a.display_name
            ORDER BY score DESC, s.song_id ASC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public RecommendationServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<SongRecommendationResponse> similarSongs(Long songId, int limit) {
        LOGGER.info("Fetching similar songs for songId={}, limit={}", songId, limit);
        if (songId == null) {
            throw new PlaybackValidationException("songId is required");
        }
        PlaybackValidationUtil.requireLimitInRange(limit, 1, 100);
        try {
            return jdbcTemplate.query(SIMILAR_SONGS_SQL, (rs, rowNum) -> new SongRecommendationResponse(
                    rs.getLong("song_id"),
                    rs.getString("title"),
                    rs.getLong("artist_id"),
                    rs.getString("artist_name"),
                    rs.getLong("score")
            ), songId, limit);
        } catch (DataAccessException ex) {
            LOGGER.error("Similar songs query failed for songId={}", songId, ex);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public ForYouRecommendationsResponse forUser(Long userId, int limit) {
        LOGGER.info("Fetching for-you recommendations for userId={}, limit={}", userId, limit);
        if (userId == null) {
            throw new PlaybackValidationException("userId is required");
        }
        PlaybackValidationUtil.requireLimitInRange(limit, 1, 100);

        try {
            List<SongRecommendationResponse> youMightLike = jdbcTemplate.query(
                    YOU_MIGHT_LIKE_SQL,
                    (rs, rowNum) -> new SongRecommendationResponse(
                            rs.getLong("song_id"),
                            rs.getString("title"),
                            rs.getLong("artist_id"),
                            rs.getString("artist_name"),
                            rs.getLong("score")
                    ),
                    userId,
                    userId,
                    limit
            );

            List<SongRecommendationResponse> popularWithSimilarUsers = jdbcTemplate.query(
                    POPULAR_WITH_SIMILAR_USERS_SQL,
                    (rs, rowNum) -> new SongRecommendationResponse(
                            rs.getLong("song_id"),
                            rs.getString("title"),
                            rs.getLong("artist_id"),
                            rs.getString("artist_name"),
                            rs.getLong("score")
                    ),
                    userId,
                    userId,
                    userId,
                    limit
            );

            return new ForYouRecommendationsResponse(userId, youMightLike, popularWithSimilarUsers);
        } catch (DataAccessException ex) {
            LOGGER.error("For-you recommendation query failed for userId={}", userId, ex);
            return new ForYouRecommendationsResponse(userId, Collections.emptyList(), Collections.emptyList());
        }
    }
}





