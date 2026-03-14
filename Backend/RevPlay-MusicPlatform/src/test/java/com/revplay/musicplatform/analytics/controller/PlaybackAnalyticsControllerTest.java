package com.revplay.musicplatform.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.analytics.dto.response.ActiveUsersMetricsResponse;
import com.revplay.musicplatform.analytics.dto.response.ContentPerformanceResponse;
import com.revplay.musicplatform.analytics.dto.response.DashboardMetricsResponse;
import com.revplay.musicplatform.analytics.dto.response.GenrePlayCountResponse;
import com.revplay.musicplatform.analytics.dto.response.TopArtistResponse;
import com.revplay.musicplatform.analytics.dto.response.TrendingContentResponse;
import com.revplay.musicplatform.analytics.dto.response.UserListeningStatsResponse;
import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.service.PlaybackAnalyticsService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.security.service.PlaybackRateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlaybackAnalyticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class PlaybackAnalyticsControllerTest {

    private static final String BASE = "/api/v1/analytics";
    private static final Long USER_ID = 1L;
    private static final String PERIOD_WEEKLY = "WEEKLY";
    private static final String TYPE_SONG = "song";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_ROLE = "ADMIN";

    private final MockMvc mockMvc;

    @MockBean
    private PlaybackAnalyticsService playbackAnalyticsService;
    @MockBean
    private PlaybackRateLimiterService playbackRateLimiterService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    PlaybackAnalyticsControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(
                    invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    @DisplayName("GET dashboard metrics authenticated returns 200")
    void dashboardMetricsAuthenticated() throws Exception {
        when(playbackAnalyticsService.dashboardMetrics()).thenReturn(
                new DashboardMetricsResponse(
                        10L,
                        2L,
                        new ActiveUsersMetricsResponse(1L, 1L, 1L),
                        new ContentPerformanceResponse(
                                new TrendingContentResponse(TYPE_SONG, 1L, "S", 5L),
                                new TrendingContentResponse("podcast", 2L, "P", 4L)
                        )
                )
        );

        mockMvc.perform(get(BASE + "/dashboard-metrics").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPlatformPlays").value(10));

        verify(playbackAnalyticsService).dashboardMetrics();
    }

    @Test
    @DisplayName("GET dashboard metrics without jwt returns 403")
    void dashboardMetricsNoJwt() throws Exception {
        mockMvc.perform(get(BASE + "/dashboard-metrics"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(playbackAnalyticsService);
    }

    @Test
    @DisplayName("GET trending weekly authenticated returns 200")
    void trendingWeeklyAuthenticated() throws Exception {
        when(playbackAnalyticsService.trending(any(), any(), anyInt()))
                .thenReturn(List.of(new TrendingContentResponse(TYPE_SONG, 1L, "A", 10L)));

        mockMvc.perform(get(BASE + "/trending")
                        .param("type", TYPE_SONG)
                        .param("period", PERIOD_WEEKLY)
                        .with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value(TYPE_SONG));

        verify(playbackAnalyticsService).trending(any(), any(), anyInt());
    }

    @Test
    @DisplayName("GET top artists authenticated returns 200")
    void topArtistsAuthenticated() throws Exception {
        when(playbackAnalyticsService.topArtists(anyInt()))
                .thenReturn(List.of(new TopArtistResponse(1L, "Artist A", 100L)));

        mockMvc.perform(get(BASE + "/top-artists").param("limit", "5").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].displayName").value("Artist A"));

        verify(playbackAnalyticsService).topArtists(5);
    }

    @Test
    @DisplayName("GET top content authenticated returns 200")
    void topContentAuthenticated() throws Exception {
        when(playbackAnalyticsService.topContent(any(), anyInt()))
                .thenReturn(List.of(new TrendingContentResponse(TYPE_SONG, 5L, "Song X", 20L)));

        mockMvc.perform(get(BASE + "/top-content")
                        .param("type", TYPE_SONG)
                        .param("limit", "3")
                        .with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].contentId").value(5));

        verify(playbackAnalyticsService).topContent(TYPE_SONG, 3);
    }

    @Test
    @DisplayName("GET user stats authenticated returns 200")
    void userStatsAuthenticated() throws Exception {
        UserStatisticsResponse base = new UserStatisticsResponse(USER_ID, 1L, 2L, 3L, 4L, Instant.now());
        UserListeningStatsResponse stats = new UserListeningStatsResponse(base, List.of(new GenrePlayCountResponse("Pop", 7L)), 18);
        when(playbackAnalyticsService.userStats(anyLong())).thenReturn(stats);

        mockMvc.perform(get(BASE + "/user-stats/1").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.peakListeningHour").value(18));

        verify(playbackAnalyticsService).userStats(USER_ID);
    }
}
