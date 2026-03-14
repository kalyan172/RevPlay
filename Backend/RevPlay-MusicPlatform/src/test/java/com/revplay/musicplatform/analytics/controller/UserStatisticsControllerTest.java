package com.revplay.musicplatform.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.service.UserStatisticsService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.time.Instant;
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

@WebMvcTest(UserStatisticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class UserStatisticsControllerTest {

    private static final String BASE_PATH = "/api/v1/user-statistics";
    private static final Long USER_ID = 5L;
    private static final String USERNAME = "listener";
    private static final String ROLE_LISTENER = "LISTENER";

    private final MockMvc mockMvc;

    @MockBean
    private UserStatisticsService userStatisticsService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    UserStatisticsControllerTest(MockMvc mockMvc) {
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
    @DisplayName("GET user statistics with auth returns 200")
    void getByUserIdWithAuth() throws Exception {
        when(userStatisticsService.getByUserId(USER_ID)).thenReturn(stats(USER_ID));

        mockMvc.perform(get(BASE_PATH + "/5").with(user(USERNAME).roles(ROLE_LISTENER)))
                .andExpect(status().isOk());

        verify(userStatisticsService).getByUserId(USER_ID);
    }

    @Test
    @DisplayName("POST refresh statistics with auth returns 200")
    void refreshWithAuth() throws Exception {
        when(userStatisticsService.refreshAndGet(USER_ID)).thenReturn(stats(USER_ID));

        mockMvc.perform(post(BASE_PATH + "/5/refresh").with(user(USERNAME).roles(ROLE_LISTENER)))
                .andExpect(status().isOk());

        verify(userStatisticsService).refreshAndGet(USER_ID);
    }

    @Test
    @DisplayName("GET user statistics without auth returns 403")
    void getByUserIdNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/5"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userStatisticsService);
    }

    private UserStatisticsResponse stats(Long userId) {
        return new UserStatisticsResponse(userId, 1L, 2L, 3L, 4L, Instant.parse("2026-01-01T00:00:00Z"));
    }
}
