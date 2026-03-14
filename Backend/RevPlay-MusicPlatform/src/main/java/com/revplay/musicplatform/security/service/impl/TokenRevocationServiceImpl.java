package com.revplay.musicplatform.security.service.impl;

import com.revplay.musicplatform.security.service.TokenRevocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRevocationServiceImpl.class);

    private final Map<String, Instant> revokedTokensByExpiry = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> issuedTokensByUser = new ConcurrentHashMap<>();
    private final Map<String, Instant> issuedTokenExpiry = new ConcurrentHashMap<>();

    public void registerIssuedToken(Long userId, String token, Instant expiry) {
        if (userId == null || token == null || token.isBlank() || expiry == null) {
            return;
        }
        LOGGER.debug("Registering issued token for userId={}", userId);
        issuedTokensByUser.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(token);
        issuedTokenExpiry.put(token, expiry);
        cleanupExpired();
    }

    public void revoke(String token, Instant expiry) {
        if (token == null || token.isBlank() || expiry == null) {
            return;
        }
        LOGGER.debug("Revoking token");
        revokedTokensByExpiry.put(token, expiry);
        cleanupExpired();
    }

    public boolean isRevoked(String token) {
        Instant expiry = revokedTokensByExpiry.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            revokedTokensByExpiry.remove(token);
            return false;
        }
        return true;
    }

    public void revokeAllForUser(Long userId) {
        if (userId == null) {
            return;
        }
        Set<String> tokens = issuedTokensByUser.getOrDefault(userId, new HashSet<>());
        for (String token : tokens) {
            Instant expiry = issuedTokenExpiry.get(token);
            if (expiry != null) {
                revokedTokensByExpiry.put(token, expiry);
            }
        }
        issuedTokensByUser.remove(userId);
        cleanupExpired();
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        revokedTokensByExpiry.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        issuedTokenExpiry.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        issuedTokensByUser.values().forEach(tokens -> tokens.removeIf(token -> !issuedTokenExpiry.containsKey(token)));
        issuedTokensByUser.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}


