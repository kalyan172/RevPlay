package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.dto.request.SearchRequest;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.enums.SearchContentType;
import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
class SearchServiceImplTest {

    private static final int PAGE = 0;
    private static final int SIZE = 20;
    private static final String QUERY = "echo";
    private static final String SONG_TYPE = "song";
    private static final Long SONG_ID = 1L;
    private static final Long ARTIST_ID = 2L;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SearchServiceImpl(jdbcTemplate);
        stubTableChecks(true, true, true, true, true, true);
    }

    @Test
    @DisplayName("search with query returns combined results for all types")
    void searchWithQueryAll() {
        stubAllCounts(1L, 0L, 0L, 0L, 0L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(songResult("Echo")));
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of());

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ALL));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("search song type returns mapped content")
    void searchTypeSong() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), any(), any()))
                .thenReturn(2L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(songResult("Echo")));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.SONG));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::type).containsOnly(SONG_TYPE);
        assertThat(response.getTotalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("search album type returns album content")
    void searchTypeAlbum() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM albums"), eq(Long.class), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("album", 3L, "Album", ARTIST_ID, "Artist", "MUSIC", LocalDate.now())));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ALBUM));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::type).containsOnly("album");
    }

    @Test
    @DisplayName("search artist type returns artist content")
    void searchTypeArtist() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM artists"), eq(Long.class), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("artist", 4L, "Artist", ARTIST_ID, "Artist", "MUSIC", null)));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ARTIST));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::type).containsOnly("artist");
    }

    @Test
    @DisplayName("search podcast type returns podcast content")
    void searchTypePodcast() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcasts p"), eq(Long.class), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("podcast", 5L, "Podcast", null, null, null, LocalDate.now())));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.PODCAST));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::type).containsOnly("podcast");
    }

    @Test
    @DisplayName("search podcast episode type returns episode content")
    void searchTypePodcastEpisode() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcast_episodes pe"), eq(Long.class), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("podcastEpisode", 6L, "Episode", null, "Podcast", null, LocalDate.now())));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.PODCAST_EPISODE));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::type).containsOnly("podcastEpisode");
    }

    @Test
    @DisplayName("search returns empty page when required tables are missing")
    void missingTablesReturnsEmptyPage() {
        stubTableChecks(false, true, true, true, true, true);

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.SONG));

        assertThat(response.getContent()).isEmpty();
        verify(jdbcTemplate, never()).query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("search returns empty page on data access exception")
    void dataAccessExceptionReturnsEmptyPage() {
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("db"));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.SONG));

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("empty query throws validation exception")
    void emptyQuery() {
        assertThatThrownBy(() -> service.search(new SearchRequest("", SearchContentType.ALL, null, null, null, null, PAGE, SIZE, "releaseDate", "DESC")))
                .isInstanceOf(DiscoveryValidationException.class);
    }

    @Test
    @DisplayName("invalid date range throws validation exception")
    void invalidDateRange() {
        assertThatThrownBy(() -> service.search(new SearchRequest(QUERY, SearchContentType.ALL, null, LocalDate.now(), LocalDate.now().minusDays(1), null, PAGE, SIZE, "releaseDate", "DESC")))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("releaseDateFrom must be before or equal to releaseDateTo");
    }

    @Test
    @DisplayName("invalid sortBy throws validation exception")
    void invalidSortBy() {
        assertThatThrownBy(() -> service.search(new SearchRequest(QUERY, SearchContentType.SONG, null, null, null, null, PAGE, SIZE, "bad", "DESC")))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("sortBy must be one of: releaseDate, title, contentId");
    }

    @Test
    @DisplayName("invalid sortDir throws validation exception")
    void invalidSortDir() {
        assertThatThrownBy(() -> service.search(new SearchRequest(QUERY, SearchContentType.SONG, null, null, null, null, PAGE, SIZE, "title", "DOWN")))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("sortDir must be ASC or DESC");
    }

    @Test
    @DisplayName("null type defaults to all search")
    void nullTypeDefaultsToAll() {
        stubAllCounts(1L, 0L, 0L, 0L, 0L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(songResult("Echo")));
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any())).thenReturn(List.of());

        PagedResponseDto<SearchResultItemResponse> response = service.search(new SearchRequest(QUERY, null, null, null, null, null, PAGE, SIZE, "releaseDate", "DESC"));

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("pagination values are passed to query offset and limit")
    void paginationApplied() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.search(new SearchRequest(QUERY, SearchContentType.SONG, null, null, null, null, 2, 10, "title", "ASC"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).contains("LIMIT ? OFFSET ?").contains("ORDER BY s.title ASC");
    }

    @Test
    @DisplayName("song search applies genre artist type and release filters")
    void songSearchAppliesAllFilters() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(songResult("Filtered")));

        PagedResponseDto<SearchResultItemResponse> response = service.search(
                new SearchRequest(QUERY, SearchContentType.SONG, 9L, from, to, "MUSIC", PAGE, SIZE, "contentId", "ASC"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue())
                .contains("sg.genre_id = ?")
                .contains("s.release_date >= ?")
                .contains("s.release_date <= ?")
                .contains("LOWER(a.artist_type) = ?")
                .contains("ORDER BY s.song_id ASC");
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("album search applies release and artist type filters")
    void albumSearchAppliesReleaseAndArtistTypeFilters() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 2, 1);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM albums"), eq(Long.class), any(), any(), any(), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("album", 3L, "Album", ARTIST_ID, "Artist", "MUSIC", LocalDate.now())));

        service.search(new SearchRequest(QUERY, SearchContentType.ALBUM, null, from, to, "MUSIC", PAGE, SIZE, "title", "ASC"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue())
                .contains("al.release_date >= ?")
                .contains("al.release_date <= ?")
                .contains("LOWER(a.artist_type) = ?")
                .contains("ORDER BY al.title ASC");
    }

    @Test
    @DisplayName("artist search with content id sort uses artist id column")
    void artistSearchUsesContentIdSortColumn() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM artists"), eq(Long.class), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("artist", 4L, "Artist", ARTIST_ID, "Artist", "MUSIC", null)));

        service.search(new SearchRequest(QUERY, SearchContentType.ARTIST, null, null, null, "MUSIC", PAGE, SIZE, "contentId", "DESC"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).contains("LOWER(a.artist_type) = ?").contains("ORDER BY a.artist_id DESC");
    }

    @Test
    @DisplayName("podcast search applies release filters and content id sort")
    void podcastSearchAppliesReleaseFilters() {
        LocalDate from = LocalDate.of(2023, 5, 1);
        LocalDate to = LocalDate.of(2023, 6, 1);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcasts p"), eq(Long.class), any(), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("podcast", 5L, "Podcast", null, null, null, LocalDate.now())));

        service.search(new SearchRequest(QUERY, SearchContentType.PODCAST, null, from, to, null, PAGE, SIZE, "contentId", "ASC"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue())
                .contains("DATE(p.created_at) >= ?")
                .contains("DATE(p.created_at) <= ?")
                .contains("ORDER BY p.podcast_id ASC");
    }

    @Test
    @DisplayName("podcast episode search applies title sort and release filters")
    void podcastEpisodeSearchAppliesTitleSortAndReleaseFilters() {
        LocalDate from = LocalDate.of(2022, 1, 1);
        LocalDate to = LocalDate.of(2022, 1, 2);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcast_episodes pe"), eq(Long.class), any(), any(), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("podcastEpisode", 6L, "Episode", null, "Podcast", null, LocalDate.now())));

        service.search(new SearchRequest(QUERY, SearchContentType.PODCAST_EPISODE, null, from, to, null, PAGE, SIZE, "title", "ASC"));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue())
                .contains("pe.release_date >= ?")
                .contains("pe.release_date <= ?")
                .contains("ORDER BY pe.title ASC");
    }

    @Test
    @DisplayName("all search sorts by title descending across combined results")
    void allSearchSortsCombinedResultsByTitleDescending() {
        stubAllCounts(1L, 1L, 0L, 0L, 0L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(songResult("Alpha")));
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("album", 2L, "Zulu", ARTIST_ID, "Artist", "MUSIC", LocalDate.now())));
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any())).thenReturn(List.of());

        PagedResponseDto<SearchResultItemResponse> response =
                service.search(new SearchRequest(QUERY, SearchContentType.ALL, null, null, null, null, PAGE, SIZE, "title", "DESC"));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::title).containsExactly("Zulu", "Alpha");
    }

    @Test
    @DisplayName("all search uses content id comparator when requested")
    void allSearchUsesContentIdComparator() {
        stubAllCounts(1L, 1L, 0L, 0L, 0L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse(SONG_TYPE, 5L, "Song", ARTIST_ID, "Artist", "MUSIC", LocalDate.now())));
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("album", 2L, "Album", ARTIST_ID, "Artist", "MUSIC", LocalDate.now())));
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any())).thenReturn(List.of());

        PagedResponseDto<SearchResultItemResponse> response =
                service.search(new SearchRequest(QUERY, SearchContentType.ALL, null, null, null, null, PAGE, SIZE, "contentId", "ASC"));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::contentId).containsExactly(2L, 5L);
    }

    @Test
    @DisplayName("album search treats null count as zero")
    void albumSearchTreatsNullCountAsZero() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM albums"), eq(Long.class), any(), any()))
                .thenReturn(null);
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of());

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ALBUM));

        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("album search returns empty page when tables are missing")
    void missingAlbumTablesReturnsEmptyPage() {
        stubTableChecks(true, false, true, false, true, true);

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ALBUM));

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("artist search returns empty page when artist table is missing")
    void missingArtistTablesReturnsEmptyPage() {
        stubTableChecks(true, false, true, true, true, true);

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ARTIST));

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("podcast search returns empty page when podcast table is missing")
    void missingPodcastTablesReturnsEmptyPage() {
        stubTableChecks(true, true, true, true, false, true);

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.PODCAST));

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("podcast episode search returns empty page when tables are missing")
    void missingPodcastEpisodeTablesReturnsEmptyPage() {
        stubTableChecks(true, true, true, true, false, false);

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.PODCAST_EPISODE));

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("all search returns empty page when one required table is missing")
    void missingAllSearchTablesReturnsEmptyPage() {
        stubTableChecks(true, true, false, true, true, true);

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ALL));

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("song search defaults blank sort values to release date descending")
    void songSearchBlankSortDefaults() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), any(), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(songResult("Echo")));

        PagedResponseDto<SearchResultItemResponse> response =
                service.search(new SearchRequest(QUERY, SearchContentType.SONG, null, null, null, null, PAGE, SIZE, " ", " "));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue()).contains("ORDER BY s.release_date DESC");
        assertThat(response.getSortBy()).isEqualTo("releaseDate");
        assertThat(response.getSortDir()).isEqualTo("DESC");
    }

    @Test
    @DisplayName("song search treats null count as zero")
    void songSearchTreatsNullCountAsZero() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), any(), any()))
                .thenReturn(null);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(songResult("Echo")));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.SONG));

        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("artist search treats null count as zero")
    void artistSearchTreatsNullCountAsZero() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM artists"), eq(Long.class), any()))
                .thenReturn(null);
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("artist", 4L, "Artist", ARTIST_ID, "Artist", "MUSIC", null)));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.ARTIST));

        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("podcast search treats null count as zero")
    void podcastSearchTreatsNullCountAsZero() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcasts p"), eq(Long.class), any()))
                .thenReturn(null);
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("podcast", 5L, "Podcast", null, null, null, LocalDate.now())));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.PODCAST));

        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("podcast episode search treats null count as zero")
    void podcastEpisodeSearchTreatsNullCountAsZero() {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcast_episodes pe"), eq(Long.class), any(), any()))
                .thenReturn(null);
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("podcastEpisode", 6L, "Episode", null, "Podcast", null, LocalDate.now())));

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.PODCAST_EPISODE));

        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("podcast search row mapper handles null release date and artist id")
    void podcastSearchRowMapperHandlesNullReleaseDateAndArtistId() throws Exception {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcasts p"), eq(Long.class), any()))
                .thenReturn(1L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<SearchResultItemResponse> mapper = invocation.getArgument(1);
                    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
                    when(resultSet.getString("type")).thenReturn("podcast");
                    when(resultSet.getLong("content_id")).thenReturn(7L);
                    when(resultSet.getString("title")).thenReturn("Mapped Podcast");
                    when(resultSet.getObject("artist_id")).thenReturn(null);
                    when(resultSet.getString("artist_name")).thenReturn(null);
                    when(resultSet.getString("artist_type")).thenReturn(null);
                    when(resultSet.getDate("release_date")).thenReturn(null);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        PagedResponseDto<SearchResultItemResponse> response = service.search(request(SearchContentType.PODCAST));

        assertThat(response.getContent()).singleElement().satisfies(item -> {
            assertThat(item.artistId()).isNull();
            assertThat(item.releaseDate()).isNull();
        });
    }

    @Test
    @DisplayName("all search orders null release dates last when sorting ascending")
    void allSearchOrdersNullReleaseDatesLast() {
        stubAllCounts(1L, 0L, 1L, 0L, 0L);
        when(jdbcTemplate.query(startsWith("SELECT\n    'song' AS type"), any(RowMapper.class), any(), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("song", 1L, "Song", ARTIST_ID, "Artist", "MUSIC", LocalDate.of(2024, 1, 1))));
        when(jdbcTemplate.query(startsWith("SELECT\n    'album' AS type"), any(RowMapper.class), any(), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'artist' AS type"), any(RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(new SearchResultItemResponse("artist", 2L, "Artist", ARTIST_ID, "Artist", "MUSIC", null)));
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcast' AS type"), any(RowMapper.class), any(), any(), any())).thenReturn(List.of());
        when(jdbcTemplate.query(startsWith("SELECT\n    'podcastEpisode' AS type"), any(RowMapper.class), any(), any(), any(), any())).thenReturn(List.of());

        PagedResponseDto<SearchResultItemResponse> response =
                service.search(new SearchRequest(QUERY, SearchContentType.ALL, null, null, null, null, PAGE, SIZE, "releaseDate", "ASC"));

        assertThat(response.getContent()).extracting(SearchResultItemResponse::contentId).containsExactly(1L, 2L);
    }

    private SearchRequest request(SearchContentType type) {
        return new SearchRequest(QUERY, type, null, null, null, null, PAGE, SIZE, "releaseDate", "DESC");
    }

    private SearchResultItemResponse songResult(String title) {
        return new SearchResultItemResponse(SONG_TYPE, SONG_ID, title, ARTIST_ID, "Artist", "MUSIC", LocalDate.now());
    }

    private void stubAllCounts(long songs, long albums, long artists, long podcasts, long episodes) {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), any(), any())).thenReturn(songs);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM albums"), eq(Long.class), any(), any())).thenReturn(albums);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM artists"), eq(Long.class), any())).thenReturn(artists);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcasts p"), eq(Long.class), any())).thenReturn(podcasts);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(1)\nFROM podcast_episodes pe"), eq(Long.class), any(), any())).thenReturn(episodes);
    }

    private void stubTableChecks(boolean songs, boolean artists, boolean songGenres, boolean albums, boolean podcasts, boolean podcastEpisodes) {
        lenient().when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("information_schema.tables"), eq(Long.class), eq("songs"))).thenReturn(songs ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("information_schema.tables"), eq(Long.class), eq("artists"))).thenReturn(artists ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("information_schema.tables"), eq(Long.class), eq("song_genres"))).thenReturn(songGenres ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("information_schema.tables"), eq(Long.class), eq("albums"))).thenReturn(albums ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("information_schema.tables"), eq(Long.class), eq("podcasts"))).thenReturn(podcasts ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("information_schema.tables"), eq(Long.class), eq("podcast_episodes"))).thenReturn(podcastEpisodes ? 1L : 0L);
    }
}
