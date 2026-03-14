package com.revplay.musicplatform.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.user.dto.request.UpdateProfileRequest;
import com.revplay.musicplatform.user.dto.response.UserProfileResponse;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.service.UserProfileService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
@Tag("integration")
class UserProfileControllerTest {

    private static final String BASE_URL = "/api/v1/profile";
    private static final Long USER_ID = 1L;

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private FileStorageProperties fileStorageProperties;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Autowired
    UserProfileControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
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
    @DisplayName("get profile authenticated returns 200 with profile fields")
    void getProfileAuthenticated() throws Exception {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(USER_ID, "user1", UserRole.LISTENER);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
        when(userProfileService.getProfile(eq(USER_ID), any(AuthenticatedUserPrincipal.class)))
                .thenReturn(new UserProfileResponse(USER_ID, "User One", "Bio", "pic.jpg", "IN"));

        mockMvc.perform(get(BASE_URL + "/" + USER_ID).with(authentication(authToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("User One"));
    }

    @Test
    @DisplayName("get profile without jwt returns 401")
    void getProfileNoJwt() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + USER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("update profile valid payload returns 200")
    void updateProfileValid() throws Exception {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(USER_ID, "user1", UserRole.LISTENER);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
        UpdateProfileRequest request = new UpdateProfileRequest("User One", "Updated Bio", "updated.jpg", "IN");
        when(userProfileService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class), any(AuthenticatedUserPrincipal.class)))
                .thenReturn(new UserProfileResponse(USER_ID, "User One", "Updated Bio", "updated.jpg", "IN"));

        mockMvc.perform(put(BASE_URL + "/" + USER_ID)
                        .with(authentication(authToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Updated Bio"));
    }
}
