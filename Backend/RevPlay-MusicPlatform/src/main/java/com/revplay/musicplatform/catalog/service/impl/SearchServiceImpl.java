package com.revplay.musicplatform.catalog.service.impl;

import com.revplay.musicplatform.catalog.service.SearchService;
import com.revplay.musicplatform.catalog.dto.request.SearchRequest;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.enums.SearchContentType;
import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import com.revplay.musicplatform.catalog.util.DiscoveryValidationUtil;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

    private final JdbcTemplate jdbcTemplate;

    public SearchServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PagedResponseDto<SearchResultItemResponse> search(SearchRequest request) {
        DiscoveryValidationUtil.requireNotBlank(request.query(), "q");
        DiscoveryValidationUtil.requirePageSize(request.page(), request.size());
        validateDateRange(request.releaseDateFrom(), request.releaseDateTo());

        SearchContentType type = request.type() == null ? SearchContentType.ALL : request.type();
        String normalizedQuery = "%" + request.query().trim().toLowerCase(Locale.ROOT) + "%";
        String sortBy = normalizeSortBy(request.sortBy());
        String sortDir = normalizeSortDir(request.sortDir());
        int page = request.page();
        int size = request.size();
        int offset = DiscoveryValidationUtil.safeOffset(page, size);
        LOGGER.info("Executing search type={}, page={}, size={}, offset={}", type, page, size, offset);

        if (!hasRequiredTables(type)) {
            LOGGER.warn("Search skipped because required tables are missing for type={}", type);
            return emptyPage(page, size, sortBy, sortDir);
        }

        try {
            if (type == SearchContentType.ALL) {
                return searchAcrossAllTypes(
                        normalizedQuery,
                        request.genreId(),
                        request.releaseDateFrom(),
                        request.releaseDateTo(),
                        request.artistType(),
                        page,
                        size,
                        sortBy,
                        sortDir
                );
            }

            return switch (type) {
                case SONG -> searchSongs(normalizedQuery, request.genreId(), request.releaseDateFrom(), request.releaseDateTo(),
                        request.artistType(), offset, size, page, sortBy, sortDir);
                case ALBUM -> searchAlbums(normalizedQuery, request.releaseDateFrom(), request.releaseDateTo(), request.artistType(),
                        offset, size, page, sortBy, sortDir);
                case ARTIST -> searchArtists(normalizedQuery, request.artistType(), offset, size, page, sortBy, sortDir);
                case PODCAST -> searchPodcasts(normalizedQuery, request.releaseDateFrom(), request.releaseDateTo(), offset, size, page,
                        sortBy, sortDir);
                case PODCAST_EPISODE -> searchPodcastEpisodes(
                        normalizedQuery,
                        request.releaseDateFrom(),
                        request.releaseDateTo(),
                        offset,
                        size,
                        page,
                        sortBy,
                        sortDir
                );
                default -> throw new DiscoveryValidationException("Unsupported search type");
            };
        } catch (DataAccessException ex) {
            LOGGER.error("Search query failed for type={}", type, ex);
            return emptyPage(page, size, sortBy, sortDir);
        }
    }

    private PagedResponseDto<SearchResultItemResponse> searchAcrossAllTypes(
            String query,
            Long genreId,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            String artistType,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        long total = countSongs(query, genreId, releaseDateFrom, releaseDateTo, artistType)
                + countAlbums(query, releaseDateFrom, releaseDateTo, artistType)
                + countArtists(query, artistType)
                + countPodcasts(query, releaseDateFrom, releaseDateTo)
                + countPodcastEpisodes(query, releaseDateFrom, releaseDateTo);

        int expandedLimit = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        List<SearchResultItemResponse> combined = new ArrayList<>();
        combined.addAll(runSongSearch(query, genreId, releaseDateFrom, releaseDateTo, artistType, 0, expandedLimit, sortBy, sortDir));
        combined.addAll(runAlbumSearch(query, releaseDateFrom, releaseDateTo, artistType, 0, expandedLimit, sortBy, sortDir));
        combined.addAll(runArtistSearch(query, artistType, 0, expandedLimit, sortBy, sortDir));
        combined.addAll(runPodcastSearch(query, releaseDateFrom, releaseDateTo, 0, expandedLimit, sortBy, sortDir));
        combined.addAll(runPodcastEpisodeSearch(query, releaseDateFrom, releaseDateTo, 0, expandedLimit, sortBy, sortDir));

        Comparator<SearchResultItemResponse> comparator = buildComparator(sortBy, sortDir);
        combined.sort(comparator);

        int from = Math.min(page * size, combined.size());
        int to = Math.min(from + size, combined.size());
        List<SearchResultItemResponse> items = combined.subList(from, to);

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponseDto<>(items, page, size, total, totalPages, sortBy, sortDir);
    }

    private PagedResponseDto<SearchResultItemResponse> searchSongs(
            String query,
            Long genreId,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            String artistType,
            int offset,
            int size,
            int page,
            String sortBy,
            String sortDir
    ) {
        List<SearchResultItemResponse> items = runSongSearch(
                query, genreId, releaseDateFrom, releaseDateTo, artistType, offset, size, sortBy, sortDir
        );
        long total = countSongs(query, genreId, releaseDateFrom, releaseDateTo, artistType);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponseDto<>(items, page, size, total, totalPages, sortBy, sortDir);
    }

    private PagedResponseDto<SearchResultItemResponse> searchAlbums(
            String query,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            String artistType,
            int offset,
            int size,
            int page,
            String sortBy,
            String sortDir
    ) {
        List<SearchResultItemResponse> items = runAlbumSearch(query, releaseDateFrom, releaseDateTo, artistType, offset, size,
                sortBy, sortDir);
        long total = countAlbums(query, releaseDateFrom, releaseDateTo, artistType);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponseDto<>(items, page, size, total, totalPages, sortBy, sortDir);
    }

    private PagedResponseDto<SearchResultItemResponse> searchArtists(
            String query,
            String artistType,
            int offset,
            int size,
            int page,
            String sortBy,
            String sortDir
    ) {
        List<SearchResultItemResponse> items = runArtistSearch(query, artistType, offset, size, sortBy, sortDir);
        long total = countArtists(query, artistType);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponseDto<>(items, page, size, total, totalPages, sortBy, sortDir);
    }

    private PagedResponseDto<SearchResultItemResponse> searchPodcasts(
            String query,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            int offset,
            int size,
            int page,
            String sortBy,
            String sortDir
    ) {
        List<SearchResultItemResponse> items = runPodcastSearch(query, releaseDateFrom, releaseDateTo, offset, size, sortBy, sortDir);
        long total = countPodcasts(query, releaseDateFrom, releaseDateTo);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponseDto<>(items, page, size, total, totalPages, sortBy, sortDir);
    }

    private PagedResponseDto<SearchResultItemResponse> searchPodcastEpisodes(
            String query,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            int offset,
            int size,
            int page,
            String sortBy,
            String sortDir
    ) {
        List<SearchResultItemResponse> items = runPodcastEpisodeSearch(
                query,
                releaseDateFrom,
                releaseDateTo,
                offset,
                size,
                sortBy,
                sortDir
        );
        long total = countPodcastEpisodes(query, releaseDateFrom, releaseDateTo);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PagedResponseDto<>(items, page, size, total, totalPages, sortBy, sortDir);
    }

    private List<SearchResultItemResponse> runSongSearch(
            String query,
            Long genreId,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            String artistType,
            int offset,
            int size,
            String sortBy,
            String sortDir
    ) {
        StringBuilder sql = new StringBuilder("""
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
                LEFT JOIN song_genres sg ON sg.song_id = s.song_id
                WHERE (LOWER(s.title) LIKE ? OR LOWER(a.display_name) LIKE ?)
                  AND s.is_active = true
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(query);
        appendSongFilters(sql, params, genreId, releaseDateFrom, releaseDateTo, artistType);
        sql.append(" GROUP BY s.song_id, s.title, a.artist_id, a.display_name, a.artist_type, s.release_date ");
        sql.append(" ORDER BY ").append(resolveSongSortColumn(sortBy)).append(" ").append(sortDir)
                .append(", s.song_id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapSearchRow, params.toArray());
    }

    private List<SearchResultItemResponse> runAlbumSearch(
            String query,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            String artistType,
            int offset,
            int size,
            String sortBy,
            String sortDir
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    'album' AS type,
                    al.album_id AS content_id,
                    al.title,
                    a.artist_id,
                    a.display_name AS artist_name,
                    a.artist_type,
                    al.release_date
                FROM albums al
                JOIN artists a ON a.artist_id = al.artist_id
                WHERE (LOWER(al.title) LIKE ? OR LOWER(a.display_name) LIKE ?)
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(query);
        appendReleaseAndArtistTypeFilters(sql, params, releaseDateFrom, releaseDateTo, artistType, "al.release_date", "a.artist_type");
        sql.append(" ORDER BY ").append(resolveAlbumSortColumn(sortBy)).append(" ").append(sortDir)
                .append(", al.album_id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapSearchRow, params.toArray());
    }

    private List<SearchResultItemResponse> runArtistSearch(
            String query,
            String artistType,
            int offset,
            int size,
            String sortBy,
            String sortDir
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    'artist' AS type,
                    a.artist_id AS content_id,
                    a.display_name AS title,
                    a.artist_id,
                    a.display_name AS artist_name,
                    a.artist_type,
                    NULL AS release_date
                FROM artists a
                WHERE LOWER(a.display_name) LIKE ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        if (artistType != null && !artistType.isBlank()) {
            sql.append(" AND LOWER(a.artist_type) = ? ");
            params.add(artistType.trim().toLowerCase(Locale.ROOT));
        }
        sql.append(" ORDER BY ").append(resolveArtistSortColumn(sortBy)).append(" ").append(sortDir)
                .append(", a.artist_id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapSearchRow, params.toArray());
    }

    private List<SearchResultItemResponse> runPodcastSearch(
            String query,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            int offset,
            int size,
            String sortBy,
            String sortDir
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    'podcast' AS type,
                    p.podcast_id AS content_id,
                    p.title,
                    NULL AS artist_id,
                    NULL AS artist_name,
                    NULL AS artist_type,
                    DATE(p.created_at) AS release_date
                FROM podcasts p
                WHERE LOWER(p.title) LIKE ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        appendReleaseFilters(sql, params, releaseDateFrom, releaseDateTo, "DATE(p.created_at)");
        sql.append(" ORDER BY ").append(resolvePodcastSortColumn(sortBy)).append(" ").append(sortDir)
                .append(", p.podcast_id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapSearchRow, params.toArray());
    }

    private List<SearchResultItemResponse> runPodcastEpisodeSearch(
            String query,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            int offset,
            int size,
            String sortBy,
            String sortDir
    ) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    'podcastEpisode' AS type,
                    pe.episode_id AS content_id,
                    pe.title,
                    NULL AS artist_id,
                    p.title AS artist_name,
                    NULL AS artist_type,
                    pe.release_date
                FROM podcast_episodes pe
                JOIN podcasts p ON p.podcast_id = pe.podcast_id
                WHERE (LOWER(pe.title) LIKE ? OR LOWER(p.title) LIKE ?)
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(query);
        appendReleaseFilters(sql, params, releaseDateFrom, releaseDateTo, "pe.release_date");
        sql.append(" ORDER BY ").append(resolvePodcastEpisodeSortColumn(sortBy)).append(" ").append(sortDir)
                .append(", pe.episode_id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), this::mapSearchRow, params.toArray());
    }

    private long countSongs(String query, Long genreId, LocalDate releaseDateFrom, LocalDate releaseDateTo, String artistType) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT s.song_id)
                FROM songs s
                JOIN artists a ON a.artist_id = s.artist_id
                LEFT JOIN song_genres sg ON sg.song_id = s.song_id
                WHERE (LOWER(s.title) LIKE ? OR LOWER(a.display_name) LIKE ?)
                  AND s.is_active = true
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(query);
        appendSongFilters(sql, params, genreId, releaseDateFrom, releaseDateTo, artistType);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private long countAlbums(String query, LocalDate releaseDateFrom, LocalDate releaseDateTo, String artistType) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM albums al
                JOIN artists a ON a.artist_id = al.artist_id
                WHERE (LOWER(al.title) LIKE ? OR LOWER(a.display_name) LIKE ?)
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(query);
        appendReleaseAndArtistTypeFilters(sql, params, releaseDateFrom, releaseDateTo, artistType, "al.release_date", "a.artist_type");
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private long countArtists(String query, String artistType) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM artists a
                WHERE LOWER(a.display_name) LIKE ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        if (artistType != null && !artistType.isBlank()) {
            sql.append(" AND LOWER(a.artist_type) = ? ");
            params.add(artistType.trim().toLowerCase(Locale.ROOT));
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private long countPodcasts(String query, LocalDate releaseDateFrom, LocalDate releaseDateTo) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM podcasts p
                WHERE LOWER(p.title) LIKE ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        appendReleaseFilters(sql, params, releaseDateFrom, releaseDateTo, "DATE(p.created_at)");
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private long countPodcastEpisodes(String query, LocalDate releaseDateFrom, LocalDate releaseDateTo) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(1)
                FROM podcast_episodes pe
                JOIN podcasts p ON p.podcast_id = pe.podcast_id
                WHERE (LOWER(pe.title) LIKE ? OR LOWER(p.title) LIKE ?)
                """);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(query);
        appendReleaseFilters(sql, params, releaseDateFrom, releaseDateTo, "pe.release_date");
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private void appendSongFilters(
            StringBuilder sql,
            List<Object> params,
            Long genreId,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            String artistType
    ) {
        if (genreId != null) {
            DiscoveryValidationUtil.requirePositiveId(genreId, "genreId");
            sql.append(" AND sg.genre_id = ? ");
            params.add(genreId);
        }
        appendReleaseAndArtistTypeFilters(sql, params, releaseDateFrom, releaseDateTo, artistType, "s.release_date", "a.artist_type");
    }

    private void appendReleaseAndArtistTypeFilters(
            StringBuilder sql,
            List<Object> params,
            LocalDate releaseDateFrom,
            LocalDate releaseDateTo,
            String artistType,
            String releaseColumn,
            String artistTypeColumn
    ) {
        appendReleaseFilters(sql, params, releaseDateFrom, releaseDateTo, releaseColumn);
        if (artistType != null && !artistType.isBlank()) {
            sql.append(" AND LOWER(").append(artistTypeColumn).append(") = ? ");
            params.add(artistType.trim().toLowerCase(Locale.ROOT));
        }
    }

    private void appendReleaseFilters(StringBuilder sql, List<Object> params, LocalDate from, LocalDate to, String column) {
        if (from != null) {
            sql.append(" AND ").append(column).append(" >= ? ");
            params.add(Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND ").append(column).append(" <= ? ");
            params.add(Date.valueOf(to));
        }
    }

    private SearchResultItemResponse mapSearchRow(ResultSet rs, int rowNum) throws SQLException {
        Date releaseDate = rs.getDate("release_date");
        Long artistId = rs.getObject("artist_id") == null ? null : rs.getLong("artist_id");
        return new SearchResultItemResponse(
                rs.getString("type"),
                rs.getLong("content_id"),
                rs.getString("title"),
                artistId,
                rs.getString("artist_name"),
                rs.getString("artist_type"),
                releaseDate == null ? null : releaseDate.toLocalDate()
        );
    }

    private Comparator<SearchResultItemResponse> buildComparator(String sortBy, String sortDir) {
        Comparator<SearchResultItemResponse> comparator = switch (sortBy) {
            case "releaseDate" -> Comparator.comparing(
                    SearchResultItemResponse::releaseDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "title" -> Comparator.comparing(
                    item -> item.title() == null ? "" : item.title().toLowerCase(Locale.ROOT)
            );
            default -> Comparator.comparing(
                    SearchResultItemResponse::contentId,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        };
        if ("DESC".equals(sortDir)) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "releaseDate";
        }
        String normalized = sortBy.trim();
        if (!normalized.equals("releaseDate") && !normalized.equals("title") && !normalized.equals("contentId")) {
            throw new DiscoveryValidationException("sortBy must be one of: releaseDate, title, contentId");
        }
        return normalized;
    }

    private String normalizeSortDir(String sortDir) {
        if (sortDir == null || sortDir.isBlank()) {
            return "DESC";
        }
        String normalized = sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalized) && !"DESC".equals(normalized)) {
            throw new DiscoveryValidationException("sortDir must be ASC or DESC");
        }
        return normalized;
    }

    private String resolveSongSortColumn(String sortBy) {
        return switch (sortBy) {
            case "title" -> "s.title";
            case "contentId" -> "s.song_id";
            default -> "s.release_date";
        };
    }

    private String resolveAlbumSortColumn(String sortBy) {
        return switch (sortBy) {
            case "title" -> "al.title";
            case "contentId" -> "al.album_id";
            default -> "al.release_date";
        };
    }

    private String resolveArtistSortColumn(String sortBy) {
        return switch (sortBy) {
            case "contentId" -> "a.artist_id";
            default -> "a.display_name";
        };
    }

    private String resolvePodcastSortColumn(String sortBy) {
        return switch (sortBy) {
            case "title" -> "p.title";
            case "contentId" -> "p.podcast_id";
            default -> "p.created_at";
        };
    }

    private String resolvePodcastEpisodeSortColumn(String sortBy) {
        return switch (sortBy) {
            case "title" -> "pe.title";
            case "contentId" -> "pe.episode_id";
            default -> "pe.release_date";
        };
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new DiscoveryValidationException("releaseDateFrom must be before or equal to releaseDateTo");
        }
    }

    private boolean hasRequiredTables(SearchContentType type) {
        return switch (type) {
            case SONG -> hasTables("songs", "artists", "song_genres");
            case ALBUM -> hasTables("albums", "artists");
            case ARTIST -> hasTables("artists");
            case PODCAST -> hasTables("podcasts");
            case PODCAST_EPISODE -> hasTables("podcast_episodes", "podcasts");
            case ALL -> hasTables("songs", "albums", "artists", "podcasts", "song_genres", "podcast_episodes");
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

    private PagedResponseDto<SearchResultItemResponse> emptyPage(int page, int size, String sortBy, String sortDir) {
        return new PagedResponseDto<>(List.of(), page, size, 0, 0, sortBy, sortDir);
    }
}



