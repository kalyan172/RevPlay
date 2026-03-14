package com.revplay.musicplatform.ads.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.service.AdService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

@WebMvcTest(AdsController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class AdsControllerTest {

    private static final String BASE_PATH = "/api/v1/ads/next";
    private static final Long USER_ID = 10L;
    private static final Long SONG_ID = 20L;
    private static final String USER_ID_PARAM = "userId";
    private static final String SONG_ID_PARAM = "songId";
    private static final String LISTENER_USER = "listener";
    private static final String LISTENER_ROLE = "LISTENER";

    private final MockMvc mockMvc;

    @MockBean
    private AdService adService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    AdsControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("GET next ad with authentication returns 200 and wrapped response")
    void getNextWithAuthenticationReturns200() throws Exception {
        Ad ad = new Ad();
        ad.setId(1L);
        when(adService.getNextAd(USER_ID, SONG_ID)).thenReturn(ad);

        mockMvc.perform(get(BASE_PATH)
                .param(USER_ID_PARAM, USER_ID.toString())
                .param(SONG_ID_PARAM, SONG_ID.toString())
                .with(user(LISTENER_USER).roles(LISTENER_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(adService).getNextAd(USER_ID, SONG_ID);
    }

    @Test
    @DisplayName("GET next ad without authentication returns 403")
    void getNextWithoutAuthenticationReturns403() throws Exception {
        mockMvc.perform(get(BASE_PATH)
                .param(USER_ID_PARAM, USER_ID.toString())
                .param(SONG_ID_PARAM, SONG_ID.toString()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adService);
    }

    @Test
    @DisplayName("GET next ad with missing request parameter returns 400")
    void getNextWithMissingParameterReturns400() throws Exception {
        mockMvc.perform(get(BASE_PATH)
                .param(USER_ID_PARAM, USER_ID.toString())
                .with(user(LISTENER_USER).roles(LISTENER_ROLE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(adService);
    }

    @Test
    @DisplayName("GET next ad propagates bad request from service as 400")
    void getNextBadRequestFromServiceReturns400() throws Exception {
        when(adService.getNextAd(USER_ID, SONG_ID))
                .thenThrow(new BadRequestException("userId and songId are required"));

        mockMvc.perform(get(BASE_PATH)
                .param(USER_ID_PARAM, USER_ID.toString())
                .param(SONG_ID_PARAM, SONG_ID.toString())
                .with(user(LISTENER_USER).roles(LISTENER_ROLE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(adService).getNextAd(USER_ID, SONG_ID);
    }
}
