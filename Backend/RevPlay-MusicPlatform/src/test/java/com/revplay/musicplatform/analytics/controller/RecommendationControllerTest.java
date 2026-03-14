package com.revplay.musicplatform.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.analytics.dto.response.ForYouRecommendationsResponse;
import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;
import com.revplay.musicplatform.analytics.service.RecommendationService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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

@WebMvcTest(RecommendationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class RecommendationControllerTest {

    private static final String BASE_PATH = "/api/v1/recommendations";
    private static final Long SONG_ID = 10L;
    private static final Long USER_ID = 11L;
    private static final int LIMIT = 10;
    private static final String USERNAME = "listener";
    private static final String ROLE_LISTENER = "LISTENER";

    private final MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    RecommendationControllerTest(MockMvc mockMvc) {
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
    @DisplayName("GET similar songs with auth returns 200")
    void similarSongsWithAuth() throws Exception {
        when(recommendationService.similarSongs(SONG_ID, LIMIT))
                .thenReturn(List.of(new SongRecommendationResponse(1L, "S", 2L, "A", 9L)));

        mockMvc.perform(get(BASE_PATH + "/similar/10").with(user(USERNAME).roles(ROLE_LISTENER)))
                .andExpect(status().isOk());

        verify(recommendationService).similarSongs(SONG_ID, LIMIT);
    }

    @Test
    @DisplayName("GET similar songs without auth returns 403")
    void similarSongsNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/similar/10"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recommendationService);
    }

    @Test
    @DisplayName("GET for-you with auth returns 200")
    void forYouWithAuth() throws Exception {
        when(recommendationService.forUser(USER_ID, LIMIT))
                .thenReturn(new ForYouRecommendationsResponse(USER_ID, List.of(), List.of()));

        mockMvc.perform(get(BASE_PATH + "/for-you/11").with(user(USERNAME).roles(ROLE_LISTENER)))
                .andExpect(status().isOk());

        verify(recommendationService).forUser(USER_ID, LIMIT);
    }

    @Test
    @DisplayName("GET for-you without auth returns 403")
    void forYouNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/for-you/11"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recommendationService);
    }
}
