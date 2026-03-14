package com.revplay.musicplatform.ads.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.revplay.musicplatform.ads.entity.Ad;
import com.revplay.musicplatform.ads.service.AdminAdService;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.BadRequestException;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.exception.ResourceNotFoundException;
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
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAdController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class AdminAdControllerTest {

    private static final String BASE_PATH = "/api/v1/admin/ads";
    private static final String UPLOAD_PATH = BASE_PATH + "/upload";
    private static final Long AD_ID = 1L;
    private static final String TITLE = "Ad One";
    private static final String DURATION_SECONDS = "30";
    private static final String TITLE_PARAM = "title";
    private static final String DURATION_PARAM = "durationSeconds";
    private static final String FILE_PARAM = "file";
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_ROLE = "ADMIN";

    private final MockMvc mockMvc;

    @MockBean
    private AdminAdService adminAdService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    AdminAdControllerTest(MockMvc mockMvc) {
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
    @DisplayName("POST upload ad with authentication returns 201")
    void uploadWithAuthenticationReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(FILE_PARAM, "ad.mp3", "audio/mpeg", "data".getBytes());
        Ad ad = new Ad();
        ad.setId(AD_ID);
        when(adminAdService.uploadAd(any(String.class), any(), any(Integer.class))).thenReturn(ad);

        mockMvc.perform(multipart(UPLOAD_PATH)
                .file(file)
                .param(TITLE_PARAM, TITLE)
                .param(DURATION_PARAM, DURATION_SECONDS)
                .with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(AD_ID));

        verify(adminAdService).uploadAd(eq(TITLE), any(), eq(Integer.valueOf(DURATION_SECONDS)));
    }

    @Test
    @DisplayName("POST upload ad without authentication returns 403")
    void uploadWithoutAuthenticationReturns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(FILE_PARAM, "ad.mp3", "audio/mpeg", "data".getBytes());

        mockMvc.perform(multipart(UPLOAD_PATH)
                .file(file)
                .param(TITLE_PARAM, TITLE)
                .param(DURATION_PARAM, DURATION_SECONDS))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminAdService);
    }

    @Test
    @DisplayName("POST upload ad missing title returns 400")
    void uploadMissingTitleReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(FILE_PARAM, "ad.mp3", "audio/mpeg", "data".getBytes());

        mockMvc.perform(multipart(UPLOAD_PATH)
                .file(file)
                .param(DURATION_PARAM, DURATION_SECONDS)
                .with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(adminAdService);
    }

    @Test
    @DisplayName("POST upload ad service validation exception returns 400")
    void uploadValidationExceptionReturns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(FILE_PARAM, "ad.mp3", "audio/mpeg", "data".getBytes());
        when(adminAdService.uploadAd(any(String.class), any(), any(Integer.class)))
                .thenThrow(new BadRequestException("Only mp3 files are allowed"));

        mockMvc.perform(multipart(UPLOAD_PATH)
                .file(file)
                .param(TITLE_PARAM, TITLE)
                .param(DURATION_PARAM, DURATION_SECONDS)
                .with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(adminAdService).uploadAd(eq(TITLE), any(), eq(Integer.valueOf(DURATION_SECONDS)));
    }

    @Test
    @DisplayName("PATCH deactivate ad with authentication returns 200")
    void deactivateWithAuthenticationReturns200() throws Exception {
        when(adminAdService.deactivateAd(AD_ID)).thenReturn(new Ad());

        mockMvc.perform(patch(BASE_PATH + "/1/deactivate").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminAdService).deactivateAd(AD_ID);
    }

    @Test
    @DisplayName("PATCH deactivate ad not found returns 404")
    void deactivateNotFoundReturns404() throws Exception {
        when(adminAdService.deactivateAd(AD_ID)).thenThrow(new ResourceNotFoundException("Ad", AD_ID));

        mockMvc.perform(patch(BASE_PATH + "/1/deactivate").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(adminAdService).deactivateAd(AD_ID);
    }

    @Test
    @DisplayName("PATCH activate ad with authentication returns 200")
    void activateWithAuthenticationReturns200() throws Exception {
        when(adminAdService.activateAd(AD_ID)).thenReturn(new Ad());

        mockMvc.perform(patch(BASE_PATH + "/1/activate").with(user(ADMIN_USER).roles(ADMIN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminAdService).activateAd(AD_ID);
    }

    @Test
    @DisplayName("PATCH activate ad without authentication returns 403")
    void activateWithoutAuthenticationReturns403() throws Exception {
        mockMvc.perform(patch(BASE_PATH + "/1/activate"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminAdService);
    }
}
