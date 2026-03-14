package com.revplay.musicplatform.analytics.service.impl;

import com.revplay.musicplatform.analytics.dto.response.ArtistDashboardResponse;
import com.revplay.musicplatform.analytics.dto.response.ListeningTrendPointResponse;
import com.revplay.musicplatform.analytics.dto.response.SongPopularityResponse;
import com.revplay.musicplatform.analytics.dto.response.TopListenerResponse;
import com.revplay.musicplatform.analytics.enums.TrendRange;
import com.revplay.musicplatform.analytics.service.ArtistAnalyticsService;
import com.revplay.musicplatform.playback.dto.response.FavoritedUserResponse;
import com.revplay.musicplatform.playback.dto.response.SongPlayCountResponse;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import com.revplay.musicplatform.playback.util.PlaybackValidationUtil;
import java.sql.Timestamp;
import java.time.LocalDate;
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
public class ArtistAnalyticsServiceImpl implements ArtistAnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtistAnalyticsServiceImpl.class);

    private static final String ARTIST_EXISTS_SQL = "SELECT COUNT(1) FROM artists WHERE artist_id = ?";
    private static final String DASHBOARD_SQL = """
            SELECT
              ? AS artist_id,
              (SELECT COUNT(*) FROM songs s WHERE s.artist_id = ? AND s.is_active = true) AS total_songs,
              (SELECT COUNT(*) FROM play_history ph
               JOIN songs s ON s.song_id = ph.song_id
               WHERE s.artist_id = ? AND s.is_active = true) AS total_plays,
              (SELECT COUNT(*) FROM user_likes ul
               JOIN songs s ON s.song_id = ul.likeable_id
               WHERE ul.likeable_type = 'SONG' AND s.artist_id = ? AND s.is_active = true) AS total_favorites
            """;
    private static final String SONG_PLAY_COUNT_SQL = """
            SELECT s.song_id, s.title, COUNT(ph.play_id) AS play_count
            FROM songs s
            LEFT JOIN play_history ph ON ph.song_id = s.song_id
            WHERE s.artist_id = ? AND s.song_id = ? AND s.is_active = true
            GROUP BY s.song_id, s.title
            """;
    private static final String SONG_POPULARITY_SQL = """
            SELECT
              s.song_id,
              s.title,
              COUNT(DISTINCT ph.play_id) AS play_count,
              COUNT(DISTINCT ul.id) AS favorite_count
            FROM songs s
            LEFT JOIN play_history ph ON ph.song_id = s.song_id
            LEFT JOIN user_likes ul ON ul.likeable_id = s.song_id AND ul.likeable_type = 'SONG'
            WHERE s.artist_id = ? AND s.is_active = true
            GROUP BY s.song_id, s.title
            ORDER BY play_count DESC, favorite_count DESC, s.song_id ASC
            """;
    private static final String FAVORITED_USERS_SQL = """
            SELECT
              u.user_id,
              u.username,
              u.email,
              up.full_name,
              MAX(ul.created_at) AS favorited_at
            FROM user_likes ul
            JOIN songs s ON s.song_id = ul.likeable_id
            JOIN users u ON u.user_id = ul.user_id
            LEFT JOIN user_profiles up ON up.user_id = u.user_id
            WHERE s.artist_id = ? AND ul.likeable_type = 'SONG' AND s.is_active = true
            GROUP BY u.user_id, u.username, u.email, up.full_name
            ORDER BY favorited_at DESC
            """;
    private static final String TOP_LISTENERS_SQL = """
            SELECT
              u.user_id,
              u.username,
              u.email,
              up.full_name,
              COUNT(ph.play_id) AS play_count
            FROM play_history ph
            JOIN songs s ON s.song_id = ph.song_id
            JOIN users u ON u.user_id = ph.user_id
            LEFT JOIN user_profiles up ON up.user_id = u.user_id
            WHERE s.artist_id = ? AND s.is_active = true
            GROUP BY u.user_id, u.username, u.email, up.full_name
            ORDER BY play_count DESC, u.user_id ASC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public ArtistAnalyticsServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "artist.dashboard", key = "#artistId")
    public ArtistDashboardResponse dashboard(Long artistId) {
        LOGGER.info("Fetching artist dashboard for artistId={}", artistId);
        requireArtist(artistId);
        try {
            return jdbcTemplate.queryForObject(
                    DASHBOARD_SQL,
                    (rs, rowNum) -> new ArtistDashboardResponse(
                            rs.getLong("artist_id"),
                            rs.getLong("total_songs"),
                            rs.getLong("total_plays"),
                            rs.getLong("total_favorites")
                    ),
                    artistId, artistId, artistId, artistId
            );
        } catch (DataAccessException ex) {
            LOGGER.error("Artist dashboard query failed for artistId={}", artistId, ex);
            return new ArtistDashboardResponse(artistId, 0L, 0L, 0L);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SongPlayCountResponse songPlayCount(Long artistId, Long songId) {
        LOGGER.info("Fetching song play count for artistId={}, songId={}", artistId, songId);
        requireArtist(artistId);
        return jdbcTemplate.query(
                SONG_PLAY_COUNT_SQL,
                (rs, rowNum) -> new SongPlayCountResponse(
                        rs.getLong("song_id"),
                        rs.getString("title"),
                        rs.getLong("play_count")
                ),
                artistId,
                songId
        ).stream().findFirst().orElseThrow(
                () -> new PlaybackNotFoundException("Song " + songId + " does not belong to artist " + artistId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "artist.popularity", key = "#artistId")
    public List<SongPopularityResponse> songPopularity(Long artistId) {
        LOGGER.info("Fetching song popularity for artistId={}", artistId);
        requireArtist(artistId);
        try {
            return jdbcTemplate.query(
                    SONG_POPULARITY_SQL,
                    (rs, rowNum) -> new SongPopularityResponse(
                            rs.getLong("song_id"),
                            rs.getString("title"),
                            rs.getLong("play_count"),
                            rs.getLong("favorite_count")
                    ),
                    artistId
            );
        } catch (DataAccessException ex) {
            LOGGER.error("Song popularity query failed for artistId={}", artistId, ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoritedUserResponse> usersWhoFavoritedSongs(Long artistId) {
        LOGGER.info("Fetching users who favorited songs for artistId={}", artistId);
        requireArtist(artistId);
        try {
            return jdbcTemplate.query(
                    FAVORITED_USERS_SQL,
                    (rs, rowNum) -> new FavoritedUserResponse(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("full_name"),
                            toInstant(rs.getTimestamp("favorited_at"))
                    ),
                    artistId
            );
        } catch (DataAccessException ex) {
            LOGGER.error("Favorited users query failed for artistId={}", artistId, ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListeningTrendPointResponse> listeningTrends(Long artistId, TrendRange range, LocalDate from, LocalDate to) {
        LOGGER.info("Fetching listening trends for artistId={}, range={}, from={}, to={}", artistId, range, from, to);
        requireArtist(artistId);
        if (to.isBefore(from)) {
            throw new PlaybackValidationException("'to' date must be greater than or equal to 'from' date");
        }

        String bucketExpression = switch (range) {
            case DAILY -> "DATE(ph.played_at)";
            case WEEKLY -> "YEARWEEK(ph.played_at, 1)";
            case MONTHLY -> "DATE_FORMAT(ph.played_at, '%Y-%m')";
        };

        String sql = """
                SELECT %s AS bucket, COUNT(ph.play_id) AS play_count
                FROM play_history ph
                JOIN songs s ON s.song_id = ph.song_id
                WHERE s.artist_id = ?
                  AND s.is_active = true
                  AND ph.played_at >= ?
                  AND ph.played_at < DATE_ADD(?, INTERVAL 1 DAY)
                GROUP BY bucket
                ORDER BY bucket ASC
                """.formatted(bucketExpression);

        try {
            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new ListeningTrendPointResponse(
                            String.valueOf(rs.getObject("bucket")),
                            rs.getLong("play_count")
                    ),
                    artistId,
                    from,
                    to
            );
        } catch (DataAccessException ex) {
            LOGGER.error("Listening trends query failed for artistId={}", artistId, ex);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopListenerResponse> topListeners(Long artistId, int limit) {
        LOGGER.info("Fetching top listeners for artistId={}, limit={}", artistId, limit);
        requireArtist(artistId);
        PlaybackValidationUtil.requireLimitInRange(limit, 1, 100);

        try {
            return jdbcTemplate.query(
                    TOP_LISTENERS_SQL,
                    (rs, rowNum) -> new TopListenerResponse(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("full_name"),
                            rs.getLong("play_count")
                    ),
                    artistId,
                    limit
            );
        } catch (DataAccessException ex) {
            LOGGER.error("Top listeners query failed for artistId={}", artistId, ex);
            return Collections.emptyList();
        }
    }

    private void requireArtist(Long artistId) {
        if (artistId == null || artistId <= 0) {
            throw new PlaybackValidationException("artistId must be a positive number");
        }
        Long count = jdbcTemplate.queryForObject(ARTIST_EXISTS_SQL, Long.class, artistId);
        if ((count == null ? 0L : count) == 0L) {
            LOGGER.warn("Artist not found for artistId={}", artistId);
            throw new PlaybackNotFoundException("Artist " + artistId + " does not exist");
        }
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}

