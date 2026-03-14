package com.revplay.musicplatform.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.catalog.dto.response.NewReleaseItemResponse;
import com.revplay.musicplatform.catalog.dto.response.PopularPodcastItemResponse;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.dto.response.TopArtistItemResponse;
import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
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
class BrowseServiceImplTest {

    private static final int PAGE = 0;
    private static final int SIZE = 10;
    private static final String DESC = "DESC";

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BrowseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BrowseServiceImpl(jdbcTemplate);
        stubAllTables(true, true, true, true, true, true);
    }

    @Test
    @DisplayName("new releases returns empty page when required tables missing")
    void newReleasesTableMissing() {
        stubAllTables(false, true, true, true, true, true);

        PagedResponseDto<NewReleaseItemResponse> response = service.newReleases(PAGE, SIZE, DESC);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("new releases returns page data when query succeeds")
    void newReleasesSuccess() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of(new NewReleaseItemResponse("song", 1L, "S", 2L, "A", LocalDate.now())));
        when(jdbcTemplate.queryForObject(eq("SELECT ((SELECT COUNT(1) FROM songs WHERE is_active = true) + (SELECT COUNT(1) FROM albums))"), eq(Long.class)))
                .thenReturn(1L);

        PagedResponseDto<NewReleaseItemResponse> response = service.newReleases(PAGE, SIZE, DESC);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("new releases normalizes invalid sort dir to desc")
    void newReleasesInvalidSortDirDefaultsDesc() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(eq("SELECT ((SELECT COUNT(1) FROM songs WHERE is_active = true) + (SELECT COUNT(1) FROM albums))"), eq(Long.class)))
                .thenReturn(0L);

        PagedResponseDto<NewReleaseItemResponse> response = service.newReleases(PAGE, SIZE, "bad");

        assertThat(response.getSortDir()).isEqualTo("DESC");
    }

    @Test
    @DisplayName("new releases returns empty page on query failure")
    void newReleasesQueryFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("db"));

        PagedResponseDto<NewReleaseItemResponse> response = service.newReleases(PAGE, SIZE, DESC);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("top artists returns empty page when tables missing")
    void topArtistsTableMissing() {
        stubAllTables(true, false, true, true, true, true);

        PagedResponseDto<TopArtistItemResponse> response = service.topArtists(PAGE, SIZE);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getSortBy()).isEqualTo("playCount");
    }

    @Test
    @DisplayName("top artists returns mapped content on success")
    void topArtistsSuccess() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of(new TopArtistItemResponse(1L, "Artist", "MUSIC", 99L)));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM artists"), eq(Long.class))).thenReturn(1L);

        PagedResponseDto<TopArtistItemResponse> response = service.topArtists(PAGE, SIZE);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).displayName()).isEqualTo("Artist");
    }

    @Test
    @DisplayName("top artists returns empty page when query fails")
    void topArtistsQueryFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("db"));

        PagedResponseDto<TopArtistItemResponse> response = service.topArtists(PAGE, SIZE);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("popular podcasts returns empty page when tables missing")
    void popularPodcastsTableMissing() {
        stubAllTables(true, true, true, true, false, true);

        PagedResponseDto<PopularPodcastItemResponse> response = service.popularPodcasts(PAGE, SIZE);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("popular podcasts returns mapped content when query succeeds")
    void popularPodcastsSuccess() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of(new PopularPodcastItemResponse(1L, "P", 9L)));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM podcasts"), eq(Long.class))).thenReturn(1L);

        PagedResponseDto<PopularPodcastItemResponse> response = service.popularPodcasts(PAGE, SIZE);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("all songs returns empty page when tables missing")
    void allSongsTableMissing() {
        stubAllTables(false, true, true, true, true, true);

        PagedResponseDto<SearchResultItemResponse> response = service.allSongs(PAGE, SIZE, "releaseDate", DESC);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("all songs normalizes invalid sort and returns empty page on db error")
    void allSongsInvalidSortDbError() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("db"));

        PagedResponseDto<SearchResultItemResponse> response = service.allSongs(PAGE, SIZE, "invalid", "invalid");

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getSortDir()).isEqualTo("DESC");
    }

    @Test
    @DisplayName("all songs success uses normalized sort column")
    void allSongsSuccessUsesNormalizedSort() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of(new SearchResultItemResponse("song", 1L, "S", 2L, "A", "MUSIC", LocalDate.now())));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM songs WHERE is_active = true"), eq(Long.class))).thenReturn(1L);

        PagedResponseDto<SearchResultItemResponse> response = service.allSongs(PAGE, SIZE, "title", "ASC");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), anyInt(), anyInt());
        assertThat(sqlCaptor.getValue()).contains("ORDER BY s.title ASC");
        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("songs by genre validates genre id")
    void songsByGenreInvalidGenreId() {
        assertThatThrownBy(() -> service.songsByGenre(0L, PAGE, SIZE, "releaseDate", DESC))
                .isInstanceOf(DiscoveryValidationException.class)
                .hasMessage("genreId must be a positive number");
    }

    @Test
    @DisplayName("songs by genre returns empty page when tables missing")
    void songsByGenreTableMissing() {
        stubAllTables(true, true, false, true, true, true);

        PagedResponseDto<SearchResultItemResponse> response = service.songsByGenre(1L, PAGE, SIZE, "releaseDate", DESC);

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("songs by genre returns mapped content on success")
    void songsByGenreSuccess() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L), anyInt(), anyInt()))
                .thenReturn(List.of(new SearchResultItemResponse("song", 1L, "S", 2L, "A", "MUSIC", LocalDate.now())));
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), eq(1L))).thenReturn(1L);

        PagedResponseDto<SearchResultItemResponse> response = service.songsByGenre(1L, PAGE, SIZE, "contentId", "ASC");

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getSortDir()).isEqualTo("ASC");
    }

    @Test
    @DisplayName("songs by genre returns empty page on db error")
    void songsByGenreQueryFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("db"));

        PagedResponseDto<SearchResultItemResponse> response = service.songsByGenre(1L, PAGE, SIZE, "title", "ASC");

        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("invalid page size throws validation exception")
    void invalidPageSizeThrows() {
        assertThatThrownBy(() -> service.newReleases(-1, SIZE, DESC))
                .isInstanceOf(DiscoveryValidationException.class);
    }

    @Test
    @DisplayName("new releases uses desc when sort dir is blank")
    void newReleasesBlankSortDirDefaultsDesc() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(eq("SELECT ((SELECT COUNT(1) FROM songs WHERE is_active = true) + (SELECT COUNT(1) FROM albums))"), eq(Long.class)))
                .thenReturn(0L);

        PagedResponseDto<NewReleaseItemResponse> response = service.newReleases(PAGE, SIZE, " ");

        assertThat(response.getSortDir()).isEqualTo("DESC");
    }

    @Test
    @DisplayName("new releases treats null total as zero")
    void newReleasesNullTotalTreatsAsZero() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(eq("SELECT ((SELECT COUNT(1) FROM songs WHERE is_active = true) + (SELECT COUNT(1) FROM albums))"), eq(Long.class)))
                .thenReturn(null);

        PagedResponseDto<NewReleaseItemResponse> response = service.newReleases(PAGE, SIZE, DESC);

        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("top artists treats null total as zero")
    void topArtistsNullTotal() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of(new TopArtistItemResponse(1L, "Artist", "MUSIC", 99L)));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM artists"), eq(Long.class))).thenReturn(null);

        PagedResponseDto<TopArtistItemResponse> response = service.topArtists(PAGE, SIZE);

        assertThat(response.getTotalElements()).isZero();
        assertThat(response.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("popular podcasts returns empty page on query failure")
    void popularPodcastsQueryFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("db"));

        PagedResponseDto<PopularPodcastItemResponse> response = service.popularPodcasts(PAGE, SIZE);

        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("all songs blank sort values default to release date desc")
    void allSongsBlankSortValuesDefaultToReleaseDateDesc() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of(new SearchResultItemResponse("song", 1L, "S", 2L, "A", "MUSIC", LocalDate.now())));
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(1) FROM songs WHERE is_active = true"), eq(Long.class))).thenReturn(null);

        PagedResponseDto<SearchResultItemResponse> response = service.allSongs(PAGE, SIZE, " ", " ");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), anyInt(), anyInt());
        assertThat(sqlCaptor.getValue()).contains("ORDER BY s.release_date DESC");
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("songs by genre blank sort values default to release date desc")
    void songsByGenreBlankSortValuesDefaultToReleaseDateDesc() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L), anyInt(), anyInt()))
                .thenReturn(List.of(new SearchResultItemResponse("song", 1L, "S", 2L, "A", "MUSIC", LocalDate.now())));
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(DISTINCT s.song_id)"), eq(Long.class), eq(1L))).thenReturn(null);

        PagedResponseDto<SearchResultItemResponse> response = service.songsByGenre(1L, PAGE, SIZE, " ", " ");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), eq(1L), anyInt(), anyInt());
        assertThat(sqlCaptor.getValue()).contains("ORDER BY s.release_date DESC");
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("popular podcasts with missing podcast episodes returns empty page")
    void popularPodcastsMissingPodcastEpisodes() {
        stubAllTables(true, true, true, true, true, true);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("podcast_episodes"))).thenReturn(0L);

        PagedResponseDto<PopularPodcastItemResponse> response = service.popularPodcasts(PAGE, SIZE);

        assertThat(response.getContent()).isEmpty();
    }

    private void stubAllTables(boolean songs, boolean artists, boolean playHistory, boolean albums, boolean podcasts, boolean songGenres) {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("songs"))).thenReturn(songs ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("artists"))).thenReturn(artists ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("play_history"))).thenReturn(playHistory ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("albums"))).thenReturn(albums ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("podcasts"))).thenReturn(podcasts ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("song_genres"))).thenReturn(songGenres ? 1L : 0L);
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("podcast_episodes"))).thenReturn(1L);
    }
}

