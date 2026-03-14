package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.catalog.service.BrowseService;
import com.revplay.musicplatform.catalog.dto.response.NewReleaseItemResponse;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.catalog.dto.response.PopularPodcastItemResponse;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.dto.response.TopArtistItemResponse;
import com.revplay.musicplatform.catalog.util.DiscoveryValidationUtil;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowseServiceImpl implements BrowseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowseService.class);

    private final JdbcTemplate jdbcTemplate;

    public BrowseServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "browse.newReleases", key = "#page + ':' + #size + ':' + #sortDir")
    public PagedResponseDto<NewReleaseItemResponse> newReleases(int page, int size, String sortDir) {
        DiscoveryValidationUtil.requirePageSize(page, size);
        String direction = normalizeSortDir(sortDir);
        int offset = DiscoveryValidationUtil.safeOffset(page, size);
        if (!hasTables("songs", "albums", "artists")) {
            LOGGER.warn("Browse new releases skipped because content tables are missing");
            return emptyPage(page, size, "releaseDate", direction);
        }

        try {
            String sql = """
                SELECT * FROM (
                    SELECT
                        'song' AS type,
                        s.song_id AS content_id,
                        s.title,
                        a.artist_id,
                        a.display_name AS artist_name,
                        s.release_date
                    FROM songs s
                    JOIN artists a ON a.artist_id = s.artist_id
                    WHERE s.is_active = true
                    UNION ALL
                    SELECT
                        'album' AS type,
                        al.album_id AS content_id,
                        al.title,
                        a.artist_id,
                        a.display_name AS artist_name,
                        al.release_date
                    FROM albums al
                    JOIN artists a ON a.artist_id = al.artist_id
                ) rel
                ORDER BY rel.release_date %s, rel.content_id ASC
                LIMIT ? OFFSET ?
                """.formatted(direction);

            List<NewReleaseItemResponse> items = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new NewReleaseItemResponse(
                            rs.getString("type"),
                            rs.getLong("content_id"),
                            rs.getString("title"),
                            rs.getLong("artist_id"),
                            rs.getString("artist_name"),
                            rs.getDate("release_date") == null ? null : rs.getDate("release_date").toLocalDate()
                    ),
                    size,
                    offset
            );

            Long total = jdbcTemplate.queryForObject(
                    "SELECT ((SELECT COUNT(1) FROM songs WHERE is_active = true) + (SELECT COUNT(1) FROM albums))",
                    Long.class
            );
            long totalElements = total == null ? 0 : total;
            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
            return new PagedResponseDto<>(items, page, size, totalElements, totalPages, "releaseDate", direction);
        } catch (DataAccessException ex) {
            LOGGER.error("Browse new releases query failed", ex);
            return emptyPage(page, size, "releaseDate", direction);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "browse.topArtists", key = "#page + ':' + #size")
    public PagedResponseDto<TopArtistItemResponse> topArtists(int page, int size) {
        DiscoveryValidationUtil.requirePageSize(page, size);
        int offset = DiscoveryValidationUtil.safeOffset(page, size);
        if (!hasTables("artists", "songs", "play_history")) {
            LOGGER.warn("Browse top artists skipped because required tables are missing");
            return new PagedResponseDto<>(List.of(), page, size, 0, 0, "playCount", "DESC");
        }

        try {
            String sql = """
                SELECT
                    a.artist_id,
                    a.display_name,
                    a.artist_type,
                    COUNT(ph.play_id) AS play_count
                FROM artists a
                LEFT JOIN songs s ON s.artist_id = a.artist_id
                LEFT JOIN play_history ph ON ph.song_id = s.song_id
                GROUP BY a.artist_id, a.display_name, a.artist_type
                ORDER BY play_count DESC, a.artist_id ASC
                LIMIT ? OFFSET ?
                """;

            List<TopArtistItemResponse> items = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new TopArtistItemResponse(
                            rs.getLong("artist_id"),
                            rs.getString("display_name"),
                            rs.getString("artist_type"),
                            rs.getLong("play_count")
                    ),
                    size,
                    offset
            );

            Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM artists", Long.class);
            long totalElements = total == null ? 0 : total;
            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
            return new PagedResponseDto<>(items, page, size, totalElements, totalPages, "playCount", "DESC");
        } catch (DataAccessException ex) {
            LOGGER.error("Browse top artists query failed", ex);
            return new PagedResponseDto<>(List.of(), page, size, 0, 0, "playCount", "DESC");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "browse.popularPodcasts", key = "#page + ':' + #size")
    public PagedResponseDto<PopularPodcastItemResponse> popularPodcasts(int page, int size) {
        DiscoveryValidationUtil.requirePageSize(page, size);
        int offset = DiscoveryValidationUtil.safeOffset(page, size);
        if (!hasTables("podcasts", "podcast_episodes", "play_history")) {
            LOGGER.warn("Browse popular podcasts skipped because required tables are missing");
            return new PagedResponseDto<>(List.of(), page, size, 0, 0, "playCount", "DESC");
        }

        try {
            String sql = """
                SELECT
                    p.podcast_id,
                    p.title,
                    COUNT(ph.play_id) AS play_count
                FROM podcasts p
                LEFT JOIN podcast_episodes pe ON pe.podcast_id = p.podcast_id
                LEFT JOIN play_history ph ON ph.episode_id = pe.episode_id
                GROUP BY p.podcast_id, p.title
                ORDER BY play_count DESC, p.podcast_id ASC
                LIMIT ? OFFSET ?
                """;

            List<PopularPodcastItemResponse> items = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new PopularPodcastItemResponse(
                            rs.getLong("podcast_id"),
                            rs.getString("title"),
                            rs.getLong("play_count")
                    ),
                    size,
                    offset
            );

            Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM podcasts", Long.class);
            long totalElements = total == null ? 0 : total;
            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
            return new PagedResponseDto<>(items, page, size, totalElements, totalPages, "playCount", "DESC");
        } catch (DataAccessException ex) {
            LOGGER.error("Browse popular podcasts query failed", ex);
            return new PagedResponseDto<>(List.of(), page, size, 0, 0, "playCount", "DESC");
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "browse.allSongs", key = "#page + ':' + #size + ':' + #sortBy + ':' + #sortDir")
    public PagedResponseDto<SearchResultItemResponse> allSongs(int page, int size, String sortBy, String sortDir) {
        DiscoveryValidationUtil.requirePageSize(page, size);
        int offset = DiscoveryValidationUtil.safeOffset(page, size);
        String normalizedSortBy = normalizeSongsSortBy(sortBy);
        String normalizedSortDir = normalizeSortDir(sortDir);
        if (!hasTables("songs", "artists")) {
            LOGGER.warn("Browse all songs skipped because required tables are missing");
            return emptyPage(page, size, sortBy, normalizedSortDir);
        }

        try {
            String sql = """
                SELECT
                    'song' AS type,
                    s.song_id AS content_id,
                    s.title,
                    a.artist_id,
                    a.display_name AS artist_name,
                    a.artist_type,
                    s.release_date
                FROM songs s
                JOIN artists a ON a.artist_id = s.artist_id
                WHERE s.is_active = true
                ORDER BY %s %s, s.song_id ASC
                LIMIT ? OFFSET ?
                """.formatted(normalizedSortBy, normalizedSortDir);

            List<SearchResultItemResponse> items = jdbcTemplate.query(
                    sql,
                    this::mapSongsByGenreRow,
                    size,
                    offset
            );

            Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM songs WHERE is_active = true", Long.class);
            long totalElements = total == null ? 0 : total;
            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
            return new PagedResponseDto<>(items, page, size, totalElements, totalPages, sortBy, normalizedSortDir);
        } catch (DataAccessException ex) {
            LOGGER.error("Browse all songs query failed", ex);
            return emptyPage(page, size, sortBy, normalizedSortDir);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "browse.genreSongs", key = "#genreId + ':' + #page + ':' + #size + ':' + #sortBy + ':' + #sortDir")
    public PagedResponseDto<SearchResultItemResponse> songsByGenre(Long genreId, int page, int size, String sortBy, String sortDir) {
        DiscoveryValidationUtil.requirePositiveId(genreId, "genreId");
        DiscoveryValidationUtil.requirePageSize(page, size);
        int offset = DiscoveryValidationUtil.safeOffset(page, size);
        String normalizedSortBy = normalizeSongsByGenreSortBy(sortBy);
        String normalizedSortDir = normalizeSortDir(sortDir);
        if (!hasTables("songs", "artists", "song_genres")) {
            LOGGER.warn("Browse songs by genre skipped because required tables are missing for genreId={}", genreId);
            return emptyPage(page, size, sortBy, normalizedSortDir);
        }

        try {
            String sql = """
                SELECT
                    'song' AS type,
                    s.song_id AS content_id,
                    s.title,
                    a.artist_id,
                    a.display_name AS artist_name,
                    a.artist_type,
                    s.release_date
                FROM songs s
                JOIN artists a ON a.artist_id = s.artist_id
                JOIN song_genres sg ON sg.song_id = s.song_id
                WHERE sg.genre_id = ?
                  AND s.is_active = true
                ORDER BY %s %s, s.song_id ASC
                LIMIT ? OFFSET ?
                """.formatted(normalizedSortBy, normalizedSortDir);

            List<SearchResultItemResponse> items = jdbcTemplate.query(
                    sql,
                    this::mapSongsByGenreRow,
                    genreId,
                    size,
                    offset
            );

            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT s.song_id) FROM songs s JOIN song_genres sg ON sg.song_id = s.song_id WHERE sg.genre_id = ? AND s.is_active = true",
                    Long.class,
                    genreId
            );
            long totalElements = total == null ? 0 : total;
            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
            return new PagedResponseDto<>(items, page, size, totalElements, totalPages, sortBy, normalizedSortDir);
        } catch (DataAccessException ex) {
            LOGGER.error("Browse songs by genre query failed for genreId={}", genreId, ex);
            return emptyPage(page, size, sortBy, normalizedSortDir);
        }
    }

    private SearchResultItemResponse mapSongsByGenreRow(ResultSet rs, int rowNum) throws SQLException {
        Date releaseDate = rs.getDate("release_date");
        return new SearchResultItemResponse(
                rs.getString("type"),
                rs.getLong("content_id"),
                rs.getString("title"),
                rs.getLong("artist_id"),
                rs.getString("artist_name"),
                rs.getString("artist_type"),
                releaseDate == null ? null : releaseDate.toLocalDate()
        );
    }

    private String normalizeSortDir(String sortDir) {
        if (sortDir == null || sortDir.isBlank()) {
            return "DESC";
        }
        String normalized = sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalized) && !"DESC".equals(normalized)) {
            return "DESC";
        }
        return normalized;
    }

    private String normalizeSongsByGenreSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "s.release_date";
        }
        return switch (sortBy.trim()) {
            case "title" -> "s.title";
            case "contentId" -> "s.song_id";
            default -> "s.release_date";
        };
    }

    private String normalizeSongsSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "s.release_date";
        }
        return switch (sortBy.trim()) {
            case "title" -> "s.title";
            case "contentId" -> "s.song_id";
            default -> "s.release_date";
        };
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

    private <T> PagedResponseDto<T> emptyPage(int page, int size, String sortBy, String sortDir) {
        return new PagedResponseDto<>(List.of(), page, size, 0, 0, sortBy, sortDir);
    }
}



