package com.revplay.musicplatform.security.service.impl;

import com.revplay.musicplatform.security.service.PlaybackRateLimiterService;
import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlaybackRateLimiterServiceImpl implements PlaybackRateLimiterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybackRateLimiterService.class);

    private final Map<String, Deque<Instant>> requestWindows = new ConcurrentHashMap<>();

    public void ensureWithinLimit(String key, int maxRequests, int windowSeconds, String message) {
        LOGGER.debug("Checking playback rate limit key={}", key);
        Instant now = Instant.now();
        Deque<Instant> window = requestWindows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (window) {
            Instant threshold = now.minusSeconds(windowSeconds);
            while (!window.isEmpty() && window.peekFirst().isBefore(threshold)) {
                window.pollFirst();
            }
            if (window.size() >= maxRequests) {
                throw new PlaybackValidationException(message);
            }
            window.addLast(now);
        }
    }
}



