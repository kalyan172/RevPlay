package com.revplay.musicplatform.security.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PlaybackRateLimiterServiceImplTest {

    private static final String KEY = "playback:test";
    private static final String MESSAGE = "Too many playback requests";

    private final PlaybackRateLimiterServiceImpl service = new PlaybackRateLimiterServiceImpl();

    @Test
    @DisplayName("ensureWithinLimit allows requests up to boundary")
    void allowsUpToLimit() {
        assertThatCode(() -> {
            service.ensureWithinLimit(KEY, 2, 60, MESSAGE);
            service.ensureWithinLimit(KEY, 2, 60, MESSAGE);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ensureWithinLimit throws playback validation when limit exceeded")
    void throwsWhenExceeded() {
        service.ensureWithinLimit(KEY, 1, 60, MESSAGE);

        assertThatThrownBy(() -> service.ensureWithinLimit(KEY, 1, 60, MESSAGE))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage(MESSAGE);
    }

    @Test
    @DisplayName("different keys have independent windows")
    void keysAreIndependent() {
        service.ensureWithinLimit("k1", 1, 60, MESSAGE);

        assertThatCode(() -> service.ensureWithinLimit("k2", 1, 60, MESSAGE))
                .doesNotThrowAnyException();
    }
}
