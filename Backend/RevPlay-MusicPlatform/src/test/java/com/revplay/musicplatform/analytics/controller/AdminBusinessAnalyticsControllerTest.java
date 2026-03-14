package com.revplay.musicplatform.analytics.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.analytics.dto.response.BusinessOverviewResponse;
import com.revplay.musicplatform.analytics.dto.response.ConversionRateResponse;
import com.revplay.musicplatform.analytics.dto.response.RevenueAnalyticsResponse;
import com.revplay.musicplatform.analytics.dto.response.TopDownloadResponse;
import com.revplay.musicplatform.analytics.dto.response.TopMixResponse;
import com.revplay.musicplatform.analytics.service.AdminBusinessAnalyticsService;
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

@WebMvcTest(AdminBusinessAnalyticsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class AdminBusinessAnalyticsControllerTest {

    private static final String BASE_PATH = "/api/v1/admin/business-analytics";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final int LIMIT = 10;

    private final MockMvc mockMvc;

    @MockBean
    private AdminBusinessAnalyticsService adminBusinessAnalyticsService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    AdminBusinessAnalyticsControllerTest(MockMvc mockMvc) {
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
    @DisplayName("GET overview with auth returns 200")
    void overviewWithAuth() throws Exception {
        when(adminBusinessAnalyticsService.getBusinessOverview()).thenReturn(new BusinessOverviewResponse(1L, 1L, 1L, 1L, 1L));

        mockMvc.perform(get(BASE_PATH + "/overview").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk());

        verify(adminBusinessAnalyticsService).getBusinessOverview();
    }

    @Test
    @DisplayName("GET revenue with auth returns 200")
    void revenueWithAuth() throws Exception {
        when(adminBusinessAnalyticsService.getRevenueAnalytics()).thenReturn(new RevenueAnalyticsResponse(1.0, 2.0, 3.0));

        mockMvc.perform(get(BASE_PATH + "/revenue").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk());

        verify(adminBusinessAnalyticsService).getRevenueAnalytics();
    }

    @Test
    @DisplayName("GET top downloads with auth returns 200")
    void topDownloadsWithAuth() throws Exception {
        when(adminBusinessAnalyticsService.getTopDownloadedSongs(LIMIT)).thenReturn(List.of(new TopDownloadResponse(1L, 100L)));

        mockMvc.perform(get(BASE_PATH + "/top-downloads")
                        .param("limit", "10")
                        .with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk());

        verify(adminBusinessAnalyticsService).getTopDownloadedSongs(LIMIT);
    }

    @Test
    @DisplayName("GET top mixes with auth returns 200")
    void topMixesWithAuth() throws Exception {
        when(adminBusinessAnalyticsService.getTopMixes()).thenReturn(List.of(new TopMixResponse("Mix", 50L)));

        mockMvc.perform(get(BASE_PATH + "/top-mixes").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk());

        verify(adminBusinessAnalyticsService).getTopMixes();
    }

    @Test
    @DisplayName("GET conversion rate with auth returns 200")
    void conversionRateWithAuth() throws Exception {
        when(adminBusinessAnalyticsService.getPremiumConversionRate()).thenReturn(new ConversionRateResponse(12.3));

        mockMvc.perform(get(BASE_PATH + "/conversion-rate").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk());

        verify(adminBusinessAnalyticsService).getPremiumConversionRate();
    }

    @Test
    @DisplayName("GET overview without auth returns 403")
    void overviewNoAuth() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/overview"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminBusinessAnalyticsService);
    }
}
