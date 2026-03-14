package com.revplay.musicplatform.security.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.catalog.exception.DiscoveryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DiscoveryRateLimiterServiceImplTest {

    private static final String KEY = "discovery:test";
    private static final String MESSAGE = "Too many discovery requests";

    private final DiscoveryRateLimiterServiceImpl service = new DiscoveryRateLimiterServiceImpl();

    @Test
    @DisplayName("ensureWithinLimit allows requests up to boundary")
    void allowsUpToLimit() {
        assertThatCode(() -> {
            service.ensureWithinLimit(KEY, 2, 60, MESSAGE);
            service.ensureWithinLimit(KEY, 2, 60, MESSAGE);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ensureWithinLimit throws discovery validation when limit exceeded")
    void throwsWhenExceeded() {
        service.ensureWithinLimit(KEY, 1, 60, MESSAGE);

        assertThatThrownBy(() -> service.ensureWithinLimit(KEY, 1, 60, MESSAGE))
                .isInstanceOf(DiscoveryValidationException.class)
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
