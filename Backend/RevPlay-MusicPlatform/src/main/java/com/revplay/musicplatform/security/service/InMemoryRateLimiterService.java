package com.revplay.musicplatform.security.service;

public interface InMemoryRateLimiterService {

    void ensureWithinLimit(String key, int maxRequests, int windowSeconds, String message);
}


