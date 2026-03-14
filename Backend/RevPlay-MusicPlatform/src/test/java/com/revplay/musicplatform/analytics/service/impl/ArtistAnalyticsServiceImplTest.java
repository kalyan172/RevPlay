package com.revplay.musicplatform.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.analytics.dto.response.ArtistDashboardResponse;
import com.revplay.musicplatform.analytics.dto.response.ListeningTrendPointResponse;
import com.revplay.musicplatform.analytics.dto.response.SongPopularityResponse;
import com.revplay.musicplatform.analytics.dto.response.TopListenerResponse;
import com.revplay.musicplatform.analytics.enums.TrendRange;
import com.revplay.musicplatform.playback.dto.response.FavoritedUserResponse;
import com.revplay.musicplatform.playback.dto.response.SongPlayCountResponse;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ArtistAnalyticsServiceImplTest {

    private static final Long ARTIST_ID = 11L;
    private static final Long SONG_ID = 22L;
    private static final int LIMIT = 5;
    private static final int INVALID_LIMIT = 0;
    private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TO = LocalDate.of(2026, 1, 31);
    private static final String ARTIST_ID_VALIDATION = "artistId must be a positive number";

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ArtistAnalyticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArtistAnalyticsServiceImpl(jdbcTemplate);
    }

    @Test
    @DisplayName("dashboard returns mapped result when artist exists")
    void dashboardReturnsMappedResult() {
        ArtistDashboardResponse expected = new ArtistDashboardResponse(ARTIST_ID, 10L, 100L, 50L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(ARTIST_ID), eq(ARTIST_ID), eq(ARTIST_ID),
                eq(ARTIST_ID)))
                .thenReturn(expected);

        ArtistDashboardResponse response = service.dashboard(ARTIST_ID);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("dashboard returns zero response when dashboard query fails")
    void dashboardReturnsFallbackOnQueryFailure() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(ARTIST_ID), eq(ARTIST_ID), eq(ARTIST_ID),
                eq(ARTIST_ID)))
                .thenThrow(new DataAccessResourceFailureException("db error"));

        ArtistDashboardResponse response = service.dashboard(ARTIST_ID);

        assertThat(response.artistId()).isEqualTo(ARTIST_ID);
        assertThat(response.totalSongs()).isZero();
        assertThat(response.totalPlays()).isZero();
        assertThat(response.totalFavorites()).isZero();
    }

    @Test
    @DisplayName("dashboard rejects invalid artist id")
    void dashboardRejectsInvalidArtistId() {
        assertThatThrownBy(() -> service.dashboard(0L))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage(ARTIST_ID_VALIDATION);
    }

    @Test
    @DisplayName("dashboard throws not found when artist does not exist")
    void dashboardThrowsWhenArtistMissing() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(0L);

        assertThatThrownBy(() -> service.dashboard(ARTIST_ID))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("Artist " + ARTIST_ID + " does not exist");
    }

    @Test
    @DisplayName("songPlayCount returns first row when song belongs to artist")
    void songPlayCountReturnsRow() {
        SongPlayCountResponse expected = new SongPlayCountResponse(SONG_ID, "Track", 99L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID), eq(SONG_ID)))
                .thenReturn(List.of(expected));

        SongPlayCountResponse response = service.songPlayCount(ARTIST_ID, SONG_ID);

        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("songPlayCount throws not found when song not returned")
    void songPlayCountThrowsWhenSongMissingForArtist() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID), eq(SONG_ID)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.songPlayCount(ARTIST_ID, SONG_ID))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("Song " + SONG_ID + " does not belong to artist " + ARTIST_ID);
    }

    @Test
    @DisplayName("songPopularity returns rows when query succeeds")
    void songPopularityReturnsRows() {
        SongPopularityResponse item = new SongPopularityResponse(1L, "Song", 7L, 3L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID))).thenReturn(List.of(item));

        List<SongPopularityResponse> response = service.songPopularity(ARTIST_ID);

        assertThat(response).containsExactly(item);
    }

    @Test
    @DisplayName("songPopularity returns empty list when query fails")
    void songPopularityReturnsEmptyOnFailure() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID)))
                .thenThrow(new DataAccessResourceFailureException("db"));

        List<SongPopularityResponse> response = service.songPopularity(ARTIST_ID);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("usersWhoFavoritedSongs returns rows when query succeeds")
    @SuppressWarnings("unchecked")
    void usersWhoFavoritedSongsReturnsRows() {
        FavoritedUserResponse item = new FavoritedUserResponse(1L, "u1", "u1@x.com", "User One", Instant.now());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID))).thenReturn(List.of(item));

        List<FavoritedUserResponse> response = service.usersWhoFavoritedSongs(ARTIST_ID);

        assertThat(response).containsExactly(item);
    }

    @Test
    @DisplayName("usersWhoFavoritedSongs returns empty list when query fails")
    void usersWhoFavoritedSongsReturnsEmptyOnFailure() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID)))
                .thenThrow(new DataAccessResourceFailureException("db"));

        List<FavoritedUserResponse> response = service.usersWhoFavoritedSongs(ARTIST_ID);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("listeningTrends returns rows for valid range and dates")
    void listeningTrendsReturnsRows() {
        ListeningTrendPointResponse point = new ListeningTrendPointResponse("2026-01-01", 5L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID), eq(FROM), eq(TO)))
                .thenReturn(List.of(point));

        List<ListeningTrendPointResponse> response = service.listeningTrends(ARTIST_ID, TrendRange.DAILY, FROM, TO);

        assertThat(response).containsExactly(point);
    }

    @Test
    @DisplayName("listeningTrends rejects when to date is before from")
    void listeningTrendsRejectsInvalidDateRange() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);

        assertThatThrownBy(() -> service.listeningTrends(ARTIST_ID, TrendRange.DAILY, TO, FROM))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("'to' date must be greater than or equal to 'from' date");
    }

    @Test
    @DisplayName("listeningTrends returns empty list when query fails")
    void listeningTrendsReturnsEmptyOnFailure() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID), eq(FROM), eq(TO)))
                .thenThrow(new DataAccessResourceFailureException("db"));

        List<ListeningTrendPointResponse> response = service.listeningTrends(ARTIST_ID, TrendRange.MONTHLY, FROM, TO);

        assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("topListeners returns rows for valid limit")
    void topListenersReturnsRows() {
        TopListenerResponse item = new TopListenerResponse(1L, "u1", "u1@x.com", "User One", 12L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID), eq(LIMIT))).thenReturn(List.of(item));

        List<TopListenerResponse> response = service.topListeners(ARTIST_ID, LIMIT);

        assertThat(response).containsExactly(item);
    }

    @Test
    @DisplayName("topListeners rejects invalid limit")
    void topListenersRejectsInvalidLimit() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);

        assertThatThrownBy(() -> service.topListeners(ARTIST_ID, INVALID_LIMIT))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("limit must be between 1 and 100");
    }

    @Test
    @DisplayName("topListeners returns empty list when query fails")
    void topListenersReturnsEmptyOnFailure() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(ARTIST_ID))).thenReturn(1L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(ARTIST_ID), anyInt()))
                .thenThrow(new DataAccessResourceFailureException("db"));

        List<TopListenerResponse> response = service.topListeners(ARTIST_ID, LIMIT);

        assertThat(response).isEmpty();
    }
}
