package com.revplay.musicplatform.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.entity.UserStatistics;
import com.revplay.musicplatform.analytics.mapper.UserStatisticsMapper;
import com.revplay.musicplatform.analytics.repository.UserStatisticsRepository;
import com.revplay.musicplatform.playback.exception.PlaybackNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.BeforeEach;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserStatisticsServiceImplTest {

    private static final Long USER_ID = 42L;
    private static final String USER_EXISTS_SQL = "SELECT COUNT(1) FROM users WHERE user_id = ?";
    private static final String PLAYLISTS_SQL = "SELECT COUNT(*) FROM playlists WHERE user_id = ?";
    private static final String FAVORITES_SQL = "SELECT COUNT(*) FROM user_likes WHERE user_id = ?";
    private static final String LISTENING_TIME_SQL = "SELECT COALESCE(SUM(play_duration_seconds), 0) FROM play_history WHERE user_id = ?";
    private static final String SONGS_PLAYED_SQL = "SELECT COUNT(*) FROM play_history WHERE user_id = ? AND song_id IS NOT NULL";

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private UserStatisticsRepository repository;
    @Mock
    private UserStatisticsMapper mapper;

    private UserStatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserStatisticsServiceImpl(jdbcTemplate, repository, mapper);
    }

    @Test
    @DisplayName("getByUserId returns existing statistics without refresh")
    void getByUserIdExisting() {
        UserStatistics stats = stats(USER_ID, 1L, 2L, 3L, 4L);
        UserStatisticsResponse response = response(USER_ID, 1L, 2L, 3L, 4L);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(stats));
        when(mapper.toDto(stats)).thenReturn(response);

        UserStatisticsResponse actual = service.getByUserId(USER_ID);

        assertThat(actual).isSameAs(response);
        verify(repository).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("getByUserId refreshes and maps when snapshot missing")
    @SuppressWarnings("unchecked")
    void getByUserIdRefreshWhenMissing() {
        UserStatistics refreshed = stats(USER_ID, 5L, 6L, 7L, 8L);
        UserStatisticsResponse response = response(USER_ID, 5L, 6L, 7L, 8L);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty(), Optional.of(refreshed));
        when(jdbcTemplate.queryForObject(USER_EXISTS_SQL, Long.class, USER_ID)).thenReturn(1L);
        when(jdbcTemplate.queryForObject(PLAYLISTS_SQL, Long.class, USER_ID)).thenReturn(5L);
        when(jdbcTemplate.queryForObject(FAVORITES_SQL, Long.class, USER_ID)).thenReturn(6L);
        when(jdbcTemplate.queryForObject(LISTENING_TIME_SQL, Long.class, USER_ID)).thenReturn(7L);
        when(jdbcTemplate.queryForObject(SONGS_PLAYED_SQL, Long.class, USER_ID)).thenReturn(8L);
        when(mapper.toDto(refreshed)).thenReturn(response);

        UserStatisticsResponse actual = service.getByUserId(USER_ID);

        assertThat(actual).isSameAs(response);
        verify(jdbcTemplate).update(any(String.class), eq(USER_ID), eq(5L), eq(6L), eq(7L), eq(8L));
    }

    @Test
    @DisplayName("refreshAndGet throws not found when user does not exist")
    void refreshAndGetUserMissing() {
        when(jdbcTemplate.queryForObject(USER_EXISTS_SQL, Long.class, USER_ID)).thenReturn(0L);

        assertThatThrownBy(() -> service.refreshAndGet(USER_ID))
                .isInstanceOf(PlaybackNotFoundException.class)
                .hasMessage("User " + USER_ID + " does not exist");
    }

    @Test
    @DisplayName("refreshAndGet builds fallback snapshot when repository remains empty")
    void refreshAndGetBuildsSnapshotFallback() {
        when(jdbcTemplate.queryForObject(USER_EXISTS_SQL, Long.class, USER_ID)).thenReturn(1L);
        when(jdbcTemplate.queryForObject(PLAYLISTS_SQL, Long.class, USER_ID)).thenReturn(2L);
        when(jdbcTemplate.queryForObject(FAVORITES_SQL, Long.class, USER_ID)).thenReturn(3L);
        when(jdbcTemplate.queryForObject(LISTENING_TIME_SQL, Long.class, USER_ID)).thenReturn(4L);
        when(jdbcTemplate.queryForObject(SONGS_PLAYED_SQL, Long.class, USER_ID)).thenReturn(5L);
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        UserStatisticsResponse mapped = response(USER_ID, 2L, 3L, 4L, 5L);
        when(mapper.toDto(any(UserStatistics.class))).thenReturn(mapped);

        UserStatisticsResponse actual = service.refreshAndGet(USER_ID);

        assertThat(actual).isSameAs(mapped);
        verify(mapper).toDto(any(UserStatistics.class));
    }

    private UserStatistics stats(Long userId, Long playlists, Long favorites, Long time, Long songsPlayed) {
        UserStatistics stats = new UserStatistics();
        stats.setUserId(userId);
        stats.setTotalPlaylists(playlists);
        stats.setTotalFavoriteSongs(favorites);
        stats.setTotalListeningTimeSeconds(time);
        stats.setTotalSongsPlayed(songsPlayed);
        stats.setLastUpdated(Instant.parse("2026-01-01T00:00:00Z"));
        return stats;
    }

    private UserStatisticsResponse response(Long userId, Long playlists, Long favorites, Long time, Long songsPlayed) {
        return new UserStatisticsResponse(userId, playlists, favorites, time, songsPlayed,
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
