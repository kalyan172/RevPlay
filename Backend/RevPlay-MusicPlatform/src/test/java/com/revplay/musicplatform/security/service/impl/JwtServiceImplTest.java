package com.revplay.musicplatform.security.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.security.AuthenticatedUserPrincipal;
import com.revplay.musicplatform.security.JwtProperties;
import com.revplay.musicplatform.user.entity.User;
import com.revplay.musicplatform.user.enums.UserRole;
import com.revplay.musicplatform.user.exception.AuthUnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class JwtServiceImplTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha";
    private static final long ACCESS_EXPIRY_SECONDS = 3600L;
    private static final long REFRESH_EXPIRY_SECONDS = 1209600L;
    private static final Long USER_ID = 11L;
    private static final String USERNAME = "jwt-user";

    private final JwtServiceImpl jwtService = new JwtServiceImpl(jwtProperties());

    @Test
    @DisplayName("generateAccessToken creates parseable token with access type and claims")
    void generateAccessToken() {
        String token = jwtService.generateAccessToken(user(UserRole.ADMIN));
        Claims claims = jwtService.parseToken(token);

        assertThat(claims.get("token_type", String.class)).isEqualTo("access");
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(USER_ID));
        assertThat(claims.get("username", String.class)).isEqualTo(USERNAME);
        assertThat(claims.get("role", String.class)).isEqualTo(UserRole.ADMIN.name());
    }

    @Test
    @DisplayName("generateRefreshToken creates refresh token type")
    void generateRefreshToken() {
        String token = jwtService.generateRefreshToken(user(UserRole.LISTENER));
        assertThat(jwtService.parseToken(token).get("token_type", String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("access and refresh token type helpers return expected booleans")
    void accessRefreshHelpers() {
        String accessToken = jwtService.generateAccessToken(user(UserRole.LISTENER));
        String refreshToken = jwtService.generateRefreshToken(user(UserRole.LISTENER));

        assertThat(jwtService.isAccessToken(accessToken)).isTrue();
        assertThat(jwtService.isAccessToken(refreshToken)).isFalse();
        assertThat(jwtService.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("toPrincipal maps claims to authenticated principal")
    void toPrincipal() {
        String accessToken = jwtService.generateAccessToken(user(UserRole.ARTIST));

        AuthenticatedUserPrincipal principal = jwtService.toPrincipal(accessToken);

        assertThat(principal.userId()).isEqualTo(USER_ID);
        assertThat(principal.username()).isEqualTo(USERNAME);
        assertThat(principal.role()).isEqualTo(UserRole.ARTIST);
    }

    @Test
    @DisplayName("toPrincipal throws unauthorized for invalid role claim")
    void toPrincipalBadRoleClaim() {
        String token = signedToken("access", "BAD_ROLE", Instant.now().plusSeconds(120));
        assertThatThrownBy(() -> jwtService.toPrincipal(token))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Invalid token role claim");
    }

    @Test
    @DisplayName("parseToken throws unauthorized for tampered token")
    void parseTamperedToken() {
        String token = jwtService.generateAccessToken(user(UserRole.LISTENER));
        String tampered = tamperSignature(token);

        assertThatThrownBy(() -> jwtService.parseToken(tampered))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Invalid or expired token");
    }

    @Test
    @DisplayName("parseToken throws unauthorized for expired token")
    void parseExpiredToken() {
        String expiredToken = signedToken("access", UserRole.LISTENER.name(), Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> jwtService.parseToken(expiredToken))
                .isInstanceOf(AuthUnauthorizedException.class)
                .hasMessage("Invalid or expired token");
    }

    @Test
    @DisplayName("getExpiry returns token expiry instant")
    void getExpiry() {
        Instant expectedExpiry = Instant.now().plusSeconds(300);
        String token = signedToken("access", UserRole.LISTENER.name(), expectedExpiry);

        Instant actualExpiry = jwtService.getExpiry(token);

        assertThat(actualExpiry).isNotNull();
        assertThat(actualExpiry.getEpochSecond()).isEqualTo(expectedExpiry.getEpochSecond());
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpirationSeconds(ACCESS_EXPIRY_SECONDS);
        properties.setRefreshTokenExpirationSeconds(REFRESH_EXPIRY_SECONDS);
        return properties;
    }

    private User user(UserRole role) {
        User user = new User();
        user.setUserId(USER_ID);
        user.setUsername(USERNAME);
        user.setRole(role);
        return user;
    }

    private String signedToken(String tokenType, String role, Instant expiry) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(USER_ID))
                .claim("username", USERNAME)
                .claim("role", role)
                .claim("token_type", tokenType)
                .issuedAt(Date.from(Instant.now().minusSeconds(5)))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    private String tamperSignature(String token) {
        int lastIndex = token.length() - 1;
        char replacement = token.charAt(lastIndex) == 'a' ? 'b' : 'a';
        return token.substring(0, lastIndex) + replacement;
    }
}
