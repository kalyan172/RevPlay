package com.revplay.musicplatform.analytics.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.revplay.musicplatform.analytics.dto.response.DashboardMetricsResponse;
import com.revplay.musicplatform.analytics.dto.response.TrendingContentResponse;
import com.revplay.musicplatform.analytics.dto.response.UserListeningStatsResponse;
import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.enums.TimePeriod;
import com.revplay.musicplatform.analytics.service.UserStatisticsService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PlaybackAnalyticsServiceImplTest {

    private static final int LIMIT = 5;
    private static final Long USER_ID = 55L;

    @InjectMocks
    private PlaybackAnalyticsServiceImpl service;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserStatisticsService userStatisticsService;

    @ParameterizedTest
    @MethodSource("periodCases")
    @DisplayName("trending uses expected period cutoffs")
    void trendingUsesExpectedCutoff(TimePeriod period, long expectedDays) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(LIMIT)))
                .thenReturn(List.of(new TrendingContentResponse("song", 1L, "A", 10L)));

        Instant before = Instant.now();
        List<TrendingContentResponse> result = service.trending("song", period, LIMIT);
        Instant after = Instant.now();

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), sinceCaptor.capture(), eq(LIMIT));
        Instant lowerBound = before.minus(Duration.ofDays(expectedDays)).minusSeconds(2);
        Instant upperBound = after.minus(Duration.ofDays(expectedDays)).plusSeconds(2);
        assertThat(sinceCaptor.getValue()).isBetween(lowerBound, upperBound);
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("trending with no plays returns empty list")
    void trendingNoPlaysReturnsEmpty() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(LIMIT))).thenReturn(List.of());

        List<TrendingContentResponse> result = service.trending("song", TimePeriod.DAILY, LIMIT);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("trending podcast returns podcast results")
    void trendingPodcastReturnsResults() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(LIMIT)))
                .thenReturn(List.of(new TrendingContentResponse("podcast", 2L, "Episode", 6L)));

        List<TrendingContentResponse> result = service.trending("podcast", TimePeriod.WEEKLY, LIMIT);

        assertThat(result).extracting(TrendingContentResponse::type).containsOnly("podcast");
    }

    @Test
    @DisplayName("trending with invalid type throws validation exception")
    void trendingInvalidTypeThrows() {
        assertThatThrownBy(() -> service.trending("audiobook", TimePeriod.DAILY, LIMIT))
                .hasMessage("type must be 'song' or 'podcast'");
    }

    @Test
    @DisplayName("trending handles jdbc exceptions by returning empty list")
    void trendingJdbcExceptionReturnsEmpty() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(LIMIT)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        List<TrendingContentResponse> result = service.trending("song", TimePeriod.DAILY, LIMIT);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("dashboard metrics handles jdbc exceptions by returning zeroed metrics")
    void dashboardMetricsJdbcExceptionReturnsZeros() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        DashboardMetricsResponse metrics = service.dashboardMetrics();

        assertThat(metrics.totalPlatformPlays()).isZero();
        assertThat(metrics.playsLast24Hours()).isZero();
        assertThat(metrics.activeUsers().last7Days()).isZero();
    }

    @Test
    @DisplayName("top artists returns mapped results")
    void topArtistsReturnsMappedResults() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(LIMIT)))
                .thenReturn(List.of(new com.revplay.musicplatform.analytics.dto.response.TopArtistResponse(1L, "Artist", 12L)));

        List<com.revplay.musicplatform.analytics.dto.response.TopArtistResponse> result = service.topArtists(LIMIT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).displayName()).isEqualTo("Artist");
    }

    @Test
    @DisplayName("top artists returns empty list on jdbc failure")
    void topArtistsReturnsEmptyOnJdbcFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(LIMIT)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        List<com.revplay.musicplatform.analytics.dto.response.TopArtistResponse> result = service.topArtists(LIMIT);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("top content returns podcast results")
    void topContentPodcastReturnsResults() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(LIMIT)))
                .thenReturn(List.of(new TrendingContentResponse("podcast", 2L, "Podcast", 14L)));

        List<TrendingContentResponse> result = service.topContent("podcast", LIMIT);

        assertThat(result).extracting(TrendingContentResponse::type).containsOnly("podcast");
    }

    @Test
    @DisplayName("top content returns empty list on jdbc failure")
    void topContentReturnsEmptyOnJdbcFailure() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(LIMIT)))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        List<TrendingContentResponse> result = service.topContent("song", LIMIT);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("top content with invalid type throws validation exception")
    void topContentInvalidTypeThrows() {
        assertThatThrownBy(() -> service.topContent("radio", LIMIT))
                .hasMessage("type must be 'song' or 'podcast'");
    }

    @Test
    @DisplayName("userStats falls back to base stats when jdbc query fails")
    void userStatsFallbackOnJdbcFailure() {
        UserStatisticsResponse base = new UserStatisticsResponse(USER_ID, 0L, 0L, 0L, 0L, Instant.now());
        when(userStatisticsService.refreshAndGet(USER_ID)).thenReturn(base);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(USER_ID)))
                .thenThrow(new DataAccessResourceFailureException("db down"));
        when(userStatisticsService.getByUserId(USER_ID)).thenReturn(base);

        UserListeningStatsResponse response = service.userStats(USER_ID);

        assertThat(response.baseStatistics().userId()).isEqualTo(USER_ID);
        assertThat(response.topGenres()).isEmpty();
        assertThat(response.peakListeningHour()).isNull();
    }

    @Test
    @DisplayName("userStats returns top genres and peak hour when jdbc succeeds")
    void userStatsReturnsTopGenresAndPeakHour() {
        UserStatisticsResponse base = new UserStatisticsResponse(USER_ID, 9L, 8L, 7L, 6L, Instant.now());
        when(userStatisticsService.refreshAndGet(USER_ID)).thenReturn(base);
        when(jdbcTemplate.query(eq("SELECT g.name AS genre, COUNT(ph.play_id) AS play_count\nFROM play_history ph\nJOIN songs s ON s.song_id = ph.song_id\nJOIN song_genres sg ON sg.song_id = s.song_id\nJOIN genres g ON g.genre_id = sg.genre_id\nWHERE ph.user_id = ?\nGROUP BY g.name\nORDER BY play_count DESC\nLIMIT 5\n"), any(RowMapper.class), eq(USER_ID)))
                .thenReturn(List.of(new com.revplay.musicplatform.analytics.dto.response.GenrePlayCountResponse("Rock", 10L)));
        when(jdbcTemplate.query(eq("SELECT HOUR(played_at) AS peak_hour\nFROM play_history\nWHERE user_id = ?\nGROUP BY HOUR(played_at)\nORDER BY COUNT(*) DESC\nLIMIT 1\n"), any(ResultSetExtractor.class), eq(USER_ID)))
                .thenReturn(22);

        UserListeningStatsResponse response = service.userStats(USER_ID);

        assertThat(response.baseStatistics().userId()).isEqualTo(USER_ID);
        assertThat(response.topGenres()).hasSize(1);
        assertThat(response.peakListeningHour()).isEqualTo(22);
    }

    @Test
    @DisplayName("dashboard metrics returns mapped values on success")
    void dashboardMetricsReturnsMappedValues() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM play_history"), eq(Long.class))).thenReturn(100L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM play_history WHERE played_at >= ?"), eq(Long.class), any())).thenReturn(25L);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(DISTINCT user_id) FROM play_history WHERE played_at >= ?"), eq(Long.class), any()))
                .thenReturn(10L, 20L, 30L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(1)))
                .thenReturn(List.of(new TrendingContentResponse("song", 1L, "Top Song", 50L)))
                .thenReturn(List.of(new TrendingContentResponse("podcast", 2L, "Top Podcast", 40L)));

        DashboardMetricsResponse metrics = service.dashboardMetrics();

        assertThat(metrics.totalPlatformPlays()).isEqualTo(100L);
        assertThat(metrics.playsLast24Hours()).isEqualTo(25L);
        assertThat(metrics.activeUsers().last24Hours()).isEqualTo(10L);
        assertThat(metrics.contentPerformance().topSong().title()).isEqualTo("Top Song");
        assertThat(metrics.contentPerformance().topPodcast().title()).isEqualTo("Top Podcast");
    }

    @Test
    @DisplayName("dashboard metrics converts null counts to zero")
    void dashboardMetricsConvertsNullCountsToZero() {
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM play_history"), eq(Long.class))).thenReturn(null);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(*) FROM play_history WHERE played_at >= ?"), eq(Long.class), any())).thenReturn(null);
        when(jdbcTemplate.queryForObject(eq("SELECT COUNT(DISTINCT user_id) FROM play_history WHERE played_at >= ?"), eq(Long.class), any()))
                .thenReturn(null, null, null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(1))).thenReturn(List.of(), List.of());

        DashboardMetricsResponse metrics = service.dashboardMetrics();

        assertThat(metrics.totalPlatformPlays()).isZero();
        assertThat(metrics.playsLast24Hours()).isZero();
        assertThat(metrics.activeUsers().last30Days()).isZero();
        assertThat(metrics.contentPerformance().topSong().playCount()).isZero();
    }

    @Test
    @DisplayName("trending with same args executes jdbc twice without Spring cache context")
    void cacheHitForSameKey() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(LIMIT)))
                .thenReturn(List.of(new TrendingContentResponse("song", 1L, "A", 10L)));

        List<TrendingContentResponse> first = service.trending("song", TimePeriod.WEEKLY, LIMIT);
        List<TrendingContentResponse> second = service.trending("song", TimePeriod.WEEKLY, LIMIT);

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        verify(jdbcTemplate, times(2)).query(anyString(), any(RowMapper.class), any(), eq(LIMIT));
    }

    @Test
    @DisplayName("trending with different periods executes jdbc twice")
    void cacheMissForDifferentPeriods() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), eq(LIMIT)))
                .thenReturn(List.of(new TrendingContentResponse("song", 1L, "A", 10L)));

        service.trending("song", TimePeriod.DAILY, LIMIT);
        service.trending("song", TimePeriod.MONTHLY, LIMIT);

        verify(jdbcTemplate, times(2)).query(anyString(), any(RowMapper.class), any(), eq(LIMIT));
    }

    private static List<Arguments> periodCases() {
        return List.of(
                Arguments.of(TimePeriod.DAILY, 1L),
                Arguments.of(TimePeriod.WEEKLY, 7L),
                Arguments.of(TimePeriod.MONTHLY, 30L));
    }
}
