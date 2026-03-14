package com.revplay.musicplatform.user.dto.response;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        Long accessTokenExpiresInSeconds,
        String refreshToken,
        Long refreshTokenExpiresInSeconds,
        UserResponse user
) {
}
