package com.revplay.musicplatform.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revplay.musicplatform.exception.GlobalExceptionHandler;
import com.revplay.musicplatform.common.response.ApiResponseBodyAdvice;
import com.revplay.musicplatform.config.FileStorageProperties;
import com.revplay.musicplatform.security.JwtAuthenticationFilter;
import com.revplay.musicplatform.security.SecurityConfig;
import com.revplay.musicplatform.user.dto.request.ResendOtpRequest;
import com.revplay.musicplatform.user.dto.request.VerifyEmailOtpRequest;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;
import com.revplay.musicplatform.user.exception.AuthNotFoundException;
import com.revplay.musicplatform.user.exception.AuthValidationException;
import com.revplay.musicplatform.user.service.AuthService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthVerificationController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseBodyAdvice.class })
@Tag("integration")
class AuthVerificationControllerTest {

        private static final String BASE_URL = "/api/v1/auth";

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
        AuthVerificationControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
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
        @DisplayName("verify email valid otp returns 200")
        void verifyEmailValidOtp() throws Exception {
                when(authService.verifyEmailOtp(anyString(), anyString()))
                                .thenReturn(new SimpleMessageResponse("Email verified successfully"));

                mockMvc.perform(post(BASE_URL + "/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new VerifyEmailOtpRequest("u@revplay.com", "123456"))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("verify email invalid otp returns 400")
        void verifyEmailInvalidOtp() throws Exception {
                when(authService.verifyEmailOtp(anyString(), anyString()))
                                .thenThrow(new AuthValidationException("Invalid OTP"));
                mockMvc.perform(post(BASE_URL + "/verify-email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new VerifyEmailOtpRequest("u@revplay.com", "999999"))))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("resend otp valid email returns 200")
        void resendOtpValidEmail() throws Exception {
                when(authService.resendEmailOtp(anyString()))
                                .thenReturn(new SimpleMessageResponse("OTP sent successfully"));
                mockMvc.perform(post(BASE_URL + "/resend-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ResendOtpRequest("u@revplay.com"))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("resend otp unknown email returns 404")
        void resendOtpUnknownEmail() throws Exception {
                when(authService.resendEmailOtp(anyString()))
                                .thenThrow(new AuthNotFoundException("User not found for the given email"));
                mockMvc.perform(post(BASE_URL + "/resend-otp")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new ResendOtpRequest("missing@revplay.com"))))
                                .andExpect(status().isNotFound());
        }
}
