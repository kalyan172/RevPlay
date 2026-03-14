package com.revplay.musicplatform.user.service;

import com.revplay.musicplatform.user.dto.request.*;
import com.revplay.musicplatform.user.dto.response.AuthTokenResponse;
import com.revplay.musicplatform.user.dto.response.SimpleMessageResponse;

public interface AuthService {

    AuthTokenResponse register(RegisterRequest request);

    AuthTokenResponse login(LoginRequest request, String clientKey);

    AuthTokenResponse refreshToken(RefreshTokenRequest request);

    SimpleMessageResponse logout(String bearerToken);

    SimpleMessageResponse forgotPassword(ForgotPasswordRequest request, String clientKey);

    SimpleMessageResponse resetPassword(ResetPasswordRequest request);

    SimpleMessageResponse changePassword(Long userId, ChangePasswordRequest request);

    SimpleMessageResponse verifyEmailOtp(String email, String otp);

    SimpleMessageResponse resendEmailOtp(String email);
}


