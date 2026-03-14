package com.revplay.musicplatform.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.service.SearchService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.playlist.dto.response.PlaylistResponse;
import com.revplay.musicplatform.playlist.service.PlaylistSearchService;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.security.service.DiscoveryRateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(SearchController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class SearchControllerTest {
    private final MockMvc mockMvc;
    @MockBean private SearchService searchService;
    @MockBean private PlaylistSearchService playlistSearchService;
    @MockBean private DiscoveryRateLimiterService discoveryRateLimiterService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private FileStorageProperties fileStorageProperties;
    @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    SearchControllerTest(MockMvc mockMvc) { this.mockMvc = mockMvc; }

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0, ServletRequest.class), invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(ServletRequest.class), org.mockito.ArgumentMatchers.any(ServletResponse.class), org.mockito.ArgumentMatchers.any(FilterChain.class));
    }

    @Test
    @DisplayName("search delegates and applies rate limiter")
    void searchDelegatesAndAppliesRateLimiter() throws Exception {
        when(searchService.search(any())).thenReturn(new PagedResponseDto<>(List.of(new SearchResultItemResponse("song", 1L, "Find Me", 2L, "Artist", "MUSIC", LocalDate.now())), 0, 20, 1, 1, "releaseDate", "DESC"));
        mockMvc.perform(get("/api/v1/search").param("q", "find").header("X-Forwarded-For", "1.2.3.4")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].title").value("Find Me"));
        verify(discoveryRateLimiterService).ensureWithinLimit(eq("search:1.2.3.4"), eq(60), eq(60), any());
    }

    @Test
    @DisplayName("search uses real ip header when forwarded for is absent")
    void searchUsesRealIpHeader() throws Exception {
        when(searchService.search(any())).thenReturn(new PagedResponseDto<>(List.of(), 0, 20, 0, 0, "releaseDate", "DESC"));

        mockMvc.perform(get("/api/v1/search").param("q", "find").header("X-Real-IP", "5.6.7.8"))
                .andExpect(status().isOk());

        verify(discoveryRateLimiterService).ensureWithinLimit(eq("search:5.6.7.8"), eq(60), eq(60), any());
    }

    @Test
    @DisplayName("search falls back to remote address when headers are absent")
    void searchFallsBackToRemoteAddress() throws Exception {
        when(searchService.search(any())).thenReturn(new PagedResponseDto<>(List.of(), 0, 20, 0, 0, "releaseDate", "DESC"));

        mockMvc.perform(get("/api/v1/search").param("q", "find"))
                .andExpect(status().isOk());

        verify(discoveryRateLimiterService).ensureWithinLimit(eq("search:127.0.0.1"), eq(60), eq(60), any());
    }

    @Test
    @DisplayName("playlist search returns paged response")
    void playlistSearchReturnsPagedResponse() throws Exception {
        PlaylistResponse response = PlaylistResponse.builder().id(5L).name("Chill").userId(3L).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(playlistSearchService.searchPublicPlaylists(eq("chill"), eq(0), eq(20))).thenReturn(new PagedResponseDto<>(List.of(response), 0, 20, 1, 1, "createdAt", "DESC"));
        mockMvc.perform(get("/api/v1/search/playlists").param("keyword", "chill")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].name").value("Chill"));
    }
}
