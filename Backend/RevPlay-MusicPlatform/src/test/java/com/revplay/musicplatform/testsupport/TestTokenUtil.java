package com.revplay.musicplatform.testsupport;

import com.revplay.musicplatform.security.JwtProperties;
import com.revplay.musicplatform.security.service.impl.JwtServiceImpl;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.enums.UserRole;

public final class TestTokenUtil {

    private static final String TEST_JWT_SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha";
    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 3600L;
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 1209600L;
    private static final String TEST_PASSWORD_HASH = "test-password-hash";
    private static final String TOKEN_EMAIL_DOMAIN = "@test.local";

    private static final JwtServiceImpl JWT_SERVICE = buildJwtService();

    private TestTokenUtil() {
    }

    public static String generateAccessToken(Long userId, String username, UserRole role) {
        return JWT_SERVICE.generateAccessToken(buildTokenUser(userId, username, role));
    }

    public static String generateRefreshToken(Long userId, String username, UserRole role) {
        return JWT_SERVICE.generateRefreshToken(buildTokenUser(userId, username, role));
    }

    public static String bearerHeader(String token) {
        return "Bearer " + token;
    }

    private static JwtServiceImpl buildJwtService() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_JWT_SECRET);
        jwtProperties.setAccessTokenExpirationSeconds(ACCESS_TOKEN_EXPIRY_SECONDS);
        jwtProperties.setRefreshTokenExpirationSeconds(REFRESH_TOKEN_EXPIRY_SECONDS);
        return new JwtServiceImpl(jwtProperties);
    }

    private static User buildTokenUser(Long userId, String username, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setRole(role);
        user.setEmail(username + TOKEN_EMAIL_DOMAIN);
        user.setPasswordHash(TEST_PASSWORD_HASH);
        user.setIsActive(Boolean.TRUE);
        user.setEmailVerified(Boolean.TRUE);
        return user;
    }
}
