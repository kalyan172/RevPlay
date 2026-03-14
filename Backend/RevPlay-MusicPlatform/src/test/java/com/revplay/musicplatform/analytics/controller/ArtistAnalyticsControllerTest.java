package com.revplay.musicplatform.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.analytics.dto.response.ArtistDashboardResponse;
import com.revplay.musicplatform.analytics.dto.response.ListeningTrendPointResponse;
import com.revplay.musicplatform.analytics.dto.response.SongPopularityResponse;
import com.revplay.musicplatform.analytics.dto.response.TopListenerResponse;
import com.revplay.musicplatform.analytics.enums.TrendRange;
import com.revplay.musicplatform.analytics.service.ArtistAnalyticsService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.playback.dto.response.FavoritedUserResponse;
import com.revplay.musicplatform.playback.dto.response.SongPlayCountResponse;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.security.service.PlaybackRateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(ArtistAnalyticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class ArtistAnalyticsControllerTest {

    private static final String BASE_PATH = "/api/v1/analytics/artists";
    private static final Long ARTIST_ID = 9L;
    private static final Long SONG_ID = 4L;
    private static final int LIMIT = 10;
    private static final String USERNAME = "artist";
    private static final String ROLE_ARTIST = "ARTIST";
    private static final String CLIENT_IP = "203.0.113.11";

    private final MockMvc mockMvc;

    @MockBean
    private ArtistAnalyticsService artistAnalyticsService;
    @MockBean
    private PlaybackRateLimiterService playbackRateLimiterService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    ArtistAnalyticsControllerTest(MockMvc mockMvc) {
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
    @DisplayName("GET artist dashboard with auth returns 200 and enforces rate limit")
    void dashboardWithAuth() throws Exception {
        when(artistAnalyticsService.dashboard(ARTIST_ID)).thenReturn(new ArtistDashboardResponse(ARTIST_ID, 1L, 2L, 3L));

        mockMvc.perform(get(BASE_PATH + "/9/dashboard")
                        .header("X-Forwarded-For", CLIENT_IP)
                        .with(user(USERNAME).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(playbackRateLimiterService).ensureWithinLimit(
                eq("analytics:artist-dashboard:" + CLIENT_IP),
                eq(30),
                eq(60),
                eq("Too many analytics requests. Please try again later.")
        );
        verify(artistAnalyticsService).dashboard(ARTIST_ID);
    }

    @Test
    @DisplayName("GET song play count with auth returns 200")
    void songPlayCountWithAuth() throws Exception {
        when(artistAnalyticsService.songPlayCount(ARTIST_ID, SONG_ID)).thenReturn(new SongPlayCountResponse(SONG_ID, "S", 20L));

        mockMvc.perform(get(BASE_PATH + "/9/songs/4/plays").with(user(USERNAME).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(artistAnalyticsService).songPlayCount(ARTIST_ID, SONG_ID);
    }

    @Test
    @DisplayName("GET song popularity with auth returns 200")
    void songPopularityWithAuth() throws Exception {
        when(artistAnalyticsService.songPopularity(ARTIST_ID)).thenReturn(List.of(new SongPopularityResponse(SONG_ID, "S", 10L, 2L)));

        mockMvc.perform(get(BASE_PATH + "/9/songs/popularity").with(user(USERNAME).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(artistAnalyticsService).songPopularity(ARTIST_ID);
    }

    @Test
    @DisplayName("GET favorited users with auth returns 200")
    void favoritedUsersWithAuth() throws Exception {
        when(artistAnalyticsService.usersWhoFavoritedSongs(ARTIST_ID))
                .thenReturn(List.of(new FavoritedUserResponse(1L, "u", "e", "f", Instant.parse("2026-01-01T00:00:00Z"))));

        mockMvc.perform(get(BASE_PATH + "/9/favorites/users").with(user(USERNAME).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(artistAnalyticsService).usersWhoFavoritedSongs(ARTIST_ID);
    }

    @Test
    @DisplayName("GET trends with auth returns 200")
    void trendsWithAuth() throws Exception {
        when(artistAnalyticsService.listeningTrends(eq(ARTIST_ID), eq(TrendRange.DAILY), eq(LocalDate.parse("2026-01-01")), eq(LocalDate.parse("2026-01-10"))))
                .thenReturn(List.of(new ListeningTrendPointResponse("2026-01-01", 12L)));

        mockMvc.perform(get(BASE_PATH + "/9/trends")
                        .param("range", "DAILY")
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-10")
                        .with(user(USERNAME).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(artistAnalyticsService).listeningTrends(
                ARTIST_ID,
                TrendRange.DAILY,
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-10")
        );
    }

    @Test
    @DisplayName("GET top listeners with auth returns 200")
    void topListenersWithAuth() throws Exception {
        when(artistAnalyticsService.topListeners(ARTIST_ID, LIMIT))
                .thenReturn(List.of(new TopListenerResponse(1L, "u", "e", "f", 30L)));

        mockMvc.perform(get(BASE_PATH + "/9/top-listeners")
                        .param("limit", "10")
                        .with(user(USERNAME).roles(ROLE_ARTIST)))
                .andExpect(status().isOk());

        verify(artistAnalyticsService).topListeners(ARTIST_ID, LIMIT);
    }

    @Test
    @DisplayName("GET artist dashboard without auth returns 403")
    void dashboardNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/9/dashboard"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(artistAnalyticsService);
    }
}
