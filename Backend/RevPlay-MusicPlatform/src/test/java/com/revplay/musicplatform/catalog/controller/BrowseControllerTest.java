package com.revplay.musicplatform.catalog.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.catalog.dto.response.NewReleaseItemResponse;
import com.revplay.musicplatform.catalog.dto.response.PopularPodcastItemResponse;
import com.revplay.musicplatform.catalog.dto.response.SearchResultItemResponse;
import com.revplay.musicplatform.catalog.dto.response.TopArtistItemResponse;
import com.revplay.musicplatform.catalog.service.BrowseService;
import com.revplay.musicplatform.common.dto.PagedResponseDto;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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

@WebMvcTest(BrowseController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class BrowseControllerTest {
    private static final String BASE_PATH = "/api/v1/browse";
    private final MockMvc mockMvc;
    @MockBean private BrowseService browseService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private FileStorageProperties fileStorageProperties;
    @MockBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    BrowseControllerTest(MockMvc mockMvc) { this.mockMvc = mockMvc; }

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0, ServletRequest.class), invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(org.mockito.ArgumentMatchers.any(ServletRequest.class), org.mockito.ArgumentMatchers.any(ServletResponse.class), org.mockito.ArgumentMatchers.any(FilterChain.class));
    }

    @Test
    @DisplayName("new releases returns paged response")
    void newReleasesReturnsPagedResponse() throws Exception {
        when(browseService.newReleases(eq(0), eq(20), eq("DESC"))).thenReturn(new PagedResponseDto<>(List.of(new NewReleaseItemResponse("song", 1L, "New Song", 2L, "Artist", LocalDate.now())), 0, 20, 1, 1, "releaseDate", "DESC"));
        mockMvc.perform(get(BASE_PATH + "/new-releases")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].title").value("New Song"));
    }

    @Test
    @DisplayName("top artists returns paged response")
    void topArtistsReturnsPagedResponse() throws Exception {
        when(browseService.topArtists(eq(0), eq(20))).thenReturn(new PagedResponseDto<>(List.of(new TopArtistItemResponse(3L, "Top Artist", "MUSIC", 100L)), 0, 20, 1, 1, "contentId", "DESC"));
        mockMvc.perform(get(BASE_PATH + "/top-artists")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].displayName").value("Top Artist"));
    }

    @Test
    @DisplayName("popular podcasts returns paged response")
    void popularPodcastsReturnsPagedResponse() throws Exception {
        when(browseService.popularPodcasts(eq(0), eq(20))).thenReturn(new PagedResponseDto<>(List.of(new PopularPodcastItemResponse(4L, "Podcast", 50L)), 0, 20, 1, 1, "contentId", "DESC"));
        mockMvc.perform(get(BASE_PATH + "/popular-podcasts")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].title").value("Podcast"));
    }

    @Test
    @DisplayName("songs by genre delegates with request params")
    void songsByGenreDelegatesWithRequestParams() throws Exception {
        when(browseService.songsByGenre(eq(9L), eq(1), eq(5), eq("title"), eq("ASC"))).thenReturn(new PagedResponseDto<>(List.of(new SearchResultItemResponse("song", 1L, "Genre Song", 2L, "Artist", "MUSIC", LocalDate.now())), 1, 5, 1, 1, "title", "ASC"));
        mockMvc.perform(get(BASE_PATH + "/genres/9/songs").param("page", "1").param("size", "5").param("sortBy", "title").param("sortDir", "ASC")).andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].title").value("Genre Song"));
    }
}
