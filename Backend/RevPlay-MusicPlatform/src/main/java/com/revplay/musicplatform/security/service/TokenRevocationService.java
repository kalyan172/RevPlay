package com.revplay.musicplatform.security.service;

import java.time.Instant;

public interface TokenRevocationService {

    void registerIssuedToken(Long userId, String token, Instant expiry);

    void revoke(String token, Instant expiry);

    boolean isRevoked(String token);

    void revokeAllForUser(Long userId);
}


