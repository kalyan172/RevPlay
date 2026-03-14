package com.revplay.musicplatform.security.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class TokenRevocationServiceImplTest {

    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final String TOKEN_A1 = "token-a1";
    private static final String TOKEN_A2 = "token-a2";
    private static final String TOKEN_B1 = "token-b1";
    private static final String TOKEN_UNKNOWN = "unknown";
    private static final Instant FUTURE_EXPIRY = Instant.now().plusSeconds(300);

    private final TokenRevocationServiceImpl service = new TokenRevocationServiceImpl();

    @Test
    @DisplayName("revoke then isRevoked returns true")
    void revokeThenIsRevoked() {
        service.revoke(TOKEN_A1, FUTURE_EXPIRY);
        assertThat(service.isRevoked(TOKEN_A1)).isTrue();
    }

    @Test
    @DisplayName("isRevoked for unknown token returns false")
    void unknownTokenIsNotRevoked() {
        assertThat(service.isRevoked(TOKEN_UNKNOWN)).isFalse();
    }

    @Test
    @DisplayName("revokeAllForUser revokes all tokens for that user")
    void revokeAllForUserRevokesOnlyThatUser() {
        service.registerIssuedToken(USER_A, TOKEN_A1, FUTURE_EXPIRY);
        service.registerIssuedToken(USER_A, TOKEN_A2, FUTURE_EXPIRY);
        service.registerIssuedToken(USER_B, TOKEN_B1, FUTURE_EXPIRY);

        service.revokeAllForUser(USER_A);

        assertThat(service.isRevoked(TOKEN_A1)).isTrue();
        assertThat(service.isRevoked(TOKEN_A2)).isTrue();
        assertThat(service.isRevoked(TOKEN_B1)).isFalse();
    }

    @Test
    @DisplayName("revokeAllForUser with no tokens is no-op")
    void revokeAllNoTokensNoOp() {
        assertThatCode(() -> service.revokeAllForUser(999L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("new token registered after revokeAllForUser is not revoked")
    void registerAfterRevokeAllNotRevoked() {
        service.registerIssuedToken(USER_A, TOKEN_A1, FUTURE_EXPIRY);
        service.revokeAllForUser(USER_A);
        service.registerIssuedToken(USER_A, TOKEN_A2, FUTURE_EXPIRY);

        assertThat(service.isRevoked(TOKEN_A2)).isFalse();
    }

    @Test
    @DisplayName("revoke with null expiry does not throw")
    void revokeNullExpiryNoNpe() {
        assertThatCode(() -> service.revoke("token-null-expiry", null)).doesNotThrowAnyException();
    }
}
