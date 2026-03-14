package com.revplay.musicplatform.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.user.dto.request.ChangePasswordRequest;
import com.revplay.musicplatform.user.dto.request.ForgotPasswordRequest;
import com.revplay.musicplatform.user.dto.request.LoginRequest;
import com.revplay.musicplatform.user.dto.request.RefreshTokenRequest;
import com.revplay.musicplatform.user.dto.request.RegisterRequest;
import com.revplay.musicplatform.user.dto.request.ResetPasswordRequest;
import com.revplay.musicplatform.user.dto.response.AuthTokenResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
import com.revplay.musicplatform.user.dto.response.UserResponse;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.exception.AuthConflictException;
import com.revplay.musicplatform.user.exception.AuthUnauthorizedException;
import com.revplay.musicplatform.user.service.AuthService;
import java.time.Instant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class AuthControllerTest {

        private static final String BASE_URL = "/api/v1/auth";
        private static final String ACCESS_TOKEN = "access-token";
        private static final String REFRESH_TOKEN = "refresh-token";
        private static final String TEST_EMAIL = "u@revplay.com";
        private static final String TEST_USERNAME = "user1";
        private static final String TEST_PASSWORD = "StrongPass@123";
        private static final String TEST_NAME = "User One";
        private static final String ROLE_LISTENER = "LISTENER";
        private static final String BEARER = "Bearer";
        private static final String LOGOUT_SUCCESS = "Logged out successfully";
        private static final String RESET_SUCCESS = "Password reset successful";
        private static final Long TEST_USER_ID = 1L;

        private final MockMvc mockMvc;
        private final ObjectMapper objectMapper;

        @MockBean
        private AuthService authService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private FileStorageProperties fileStorageProperties;
        @MockBean
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;

        @Autowired
        AuthControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
                this.mockMvc = mockMvc;
                this.objectMapper = objectMapper;
        }

        @BeforeEach
        void setUp() throws Exception {
                doAnswer(invocation -> {
                        HttpServletRequest request = invocation.getArgument(0);
                        HttpServletResponse response = invocation.getArgument(1);
                        FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(request, response);
                        return null;
                }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        }

        @Test
        @DisplayName("register returns 201 with token response envelope")
        void registerValid() throws Exception {
                RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_NAME,
                                ROLE_LISTENER);
                when(authService.register(any(RegisterRequest.class))).thenReturn(tokenResponse());

                mockMvc.perform(post(BASE_URL + "/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN))
                                .andExpect(jsonPath("$.data.refreshToken").value(REFRESH_TOKEN));
        }

        @Test
        @DisplayName("register with missing email returns 400 and validation errors")
        void registerMissingEmail() throws Exception {
                String payload = String.format("{\"username\":\"%s\",\"password\":\"%s\",\"fullName\":\"%s\"}",
                                TEST_USERNAME, TEST_PASSWORD, TEST_NAME);
                mockMvc.perform(post(BASE_URL + "/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("register conflict maps to 409")
        void registerConflict() throws Exception {
                RegisterRequest request = new RegisterRequest(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_NAME,
                                ROLE_LISTENER);
                when(authService.register(any(RegisterRequest.class)))
                                .thenThrow(new AuthConflictException("Email already exists"));

                mockMvc.perform(post(BASE_URL + "/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("login valid returns 200")
        void loginValid() throws Exception {
                LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
                when(authService.login(any(LoginRequest.class), any(String.class))).thenReturn(tokenResponse());

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN));
        }

        @Test
        @DisplayName("login uses forwarded client address when header is present")
        void loginUsesForwardedClientAddress() throws Exception {
                LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
                when(authService.login(any(LoginRequest.class), any(String.class))).thenReturn(tokenResponse());

                mockMvc.perform(post(BASE_URL + "/login")
                                .header("X-Forwarded-For", "9.8.7.6, 1.1.1.1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(authService).login(any(LoginRequest.class), eq("9.8.7.6"));
        }

        @Test
        @DisplayName("login unauthorized maps to 401")
        void loginUnauthorized() throws Exception {
                LoginRequest request = new LoginRequest(TEST_EMAIL, "bad");
                when(authService.login(any(LoginRequest.class), any(String.class)))
                                .thenThrow(new AuthUnauthorizedException("Invalid credentials"));

                mockMvc.perform(post(BASE_URL + "/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("refresh valid returns 200")
        void refreshValid() throws Exception {
                when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(tokenResponse());
                mockMvc.perform(post(BASE_URL + "/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(REFRESH_TOKEN))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("refresh invalid token maps to 401")
        void refreshInvalidToken() throws Exception {
                when(authService.refreshToken(any(RefreshTokenRequest.class)))
                                .thenThrow(new AuthUnauthorizedException("Invalid refresh token"));
                mockMvc.perform(post(BASE_URL + "/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(REFRESH_TOKEN))))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("logout with header returns success")
        void logoutWithHeader() throws Exception {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(TEST_USER_ID, TEST_USERNAME,
                                UserRole.LISTENER);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));

                when(authService.logout(ACCESS_TOKEN)).thenReturn(new SimpleMessageResponse(LOGOUT_SUCCESS));
                mockMvc.perform(post(BASE_URL + "/logout")
                                .header(HttpHeaders.AUTHORIZATION, BEARER + " " + ACCESS_TOKEN)
                                .with(authentication(authenticationToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value(LOGOUT_SUCCESS));
        }

        @Test
        @DisplayName("logout with authenticated user and non bearer header passes null token to service")
        void logoutWithoutBearerHeader() throws Exception {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(TEST_USER_ID, TEST_USERNAME,
                                UserRole.LISTENER);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
                when(authService.logout(null)).thenReturn(new SimpleMessageResponse(LOGOUT_SUCCESS));

                mockMvc.perform(post(BASE_URL + "/logout")
                                .header(HttpHeaders.AUTHORIZATION, "Token " + ACCESS_TOKEN)
                                .with(authentication(authenticationToken)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value(LOGOUT_SUCCESS));

                verify(authService).logout(null);
        }

        @Test
        @DisplayName("forgot password valid request returns 200")
        void forgotPasswordValid() throws Exception {
                when(authService.forgotPassword(any(ForgotPasswordRequest.class), any(String.class)))
                                .thenReturn(new SimpleMessageResponse("Password reset email sent successfully"));
                mockMvc.perform(post(BASE_URL + "/forgot-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(TEST_EMAIL))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("reset password valid request returns 200")
        void resetPasswordValid() throws Exception {
                when(authService.resetPassword(any(ResetPasswordRequest.class)))
                                .thenReturn(new SimpleMessageResponse(RESET_SUCCESS));
                mockMvc.perform(post(BASE_URL + "/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(new ResetPasswordRequest("valid", TEST_PASSWORD))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("change password with authenticated principal returns 200")
        void changePasswordAuthenticated() throws Exception {
                AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(TEST_USER_ID, TEST_USERNAME,
                                UserRole.LISTENER);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                java.util.List.of(new SimpleGrantedAuthority("ROLE_LISTENER")));
                when(authService.changePassword(eq(TEST_USER_ID), any(ChangePasswordRequest.class)))
                                .thenReturn(new SimpleMessageResponse("Password changed successfully"));

                mockMvc.perform(post(BASE_URL + "/change-password")
                                .with(authentication(authenticationToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new ChangePasswordRequest("OldPass@123", TEST_PASSWORD))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("change password without jwt returns 403")
        void changePasswordNoJwt() throws Exception {
                mockMvc.perform(post(BASE_URL + "/change-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new ChangePasswordRequest("OldPass@123", TEST_PASSWORD))))
                                .andExpect(status().isForbidden());
        }

        private AuthTokenResponse tokenResponse() {
                return new AuthTokenResponse(
                                BEARER,
                                ACCESS_TOKEN,
                                3600L,
                                REFRESH_TOKEN,
                                1209600L,
                                new UserResponse(TEST_USER_ID, TEST_EMAIL, TEST_USERNAME, ROLE_LISTENER, true,
                                                Instant.now(),
                                                Instant.now()));
        }
}
