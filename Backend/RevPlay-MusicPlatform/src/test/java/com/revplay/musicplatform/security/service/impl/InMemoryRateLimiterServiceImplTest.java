package com.revplay.musicplatform.security.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.user.exception.AuthValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Tag("unit")
class InMemoryRateLimiterServiceImplTest {

    private static final String RATE_LIMIT_MESSAGE = "Rate limit exceeded";
    private static final int MAX_REQUESTS = 3;
    private static final int WINDOW_SECONDS = 2;
    private static final String KEY_A = "key-a";
    private static final String KEY_B = "key-b";

    private final InMemoryRateLimiterServiceImpl service = new InMemoryRateLimiterServiceImpl();

    @Test
    @DisplayName("calls below max requests do not throw")
    void belowMaxRequests() {
        assertThatCode(() -> {
            service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
            service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("boundary at max requests does not throw on nth call")
    void boundaryAtMax() {
        assertThatCode(() -> {
            service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
            service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
            service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("max plus one throws with configured message")
    void maxPlusOneThrows() {
        service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
        service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
        service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);

        assertThatThrownBy(() -> service.ensureWithinLimit(KEY_A, MAX_REQUESTS, WINDOW_SECONDS, RATE_LIMIT_MESSAGE))
                .isInstanceOf(AuthValidationException.class)
                .hasMessage(RATE_LIMIT_MESSAGE);
    }

    @Test
    @DisplayName("different keys are independent")
    void differentKeysIndependent() {
        service.ensureWithinLimit(KEY_A, 1, WINDOW_SECONDS, RATE_LIMIT_MESSAGE);
        assertThatThrownBy(() -> service.ensureWithinLimit(KEY_A, 1, WINDOW_SECONDS, RATE_LIMIT_MESSAGE))
                .isInstanceOf(AuthValidationException.class);

        assertThatCode(() -> service.ensureWithinLimit(KEY_B, 1, WINDOW_SECONDS, RATE_LIMIT_MESSAGE))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("null key currently raises null pointer exception")
    void nullKeyHandled() {
        assertThatThrownBy(() -> service.ensureWithinLimit(null, 1, WINDOW_SECONDS, RATE_LIMIT_MESSAGE))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("window resets after sleep duration")
    @Timeout(5)
    void windowReset() throws InterruptedException {
        service.ensureWithinLimit(KEY_A, 1, 1, RATE_LIMIT_MESSAGE);
        assertThatThrownBy(() -> service.ensureWithinLimit(KEY_A, 1, 1, RATE_LIMIT_MESSAGE))
                .isInstanceOf(AuthValidationException.class);

        Thread.sleep(1100);

        assertThatCode(() -> service.ensureWithinLimit(KEY_A, 1, 1, RATE_LIMIT_MESSAGE))
                .doesNotThrowAnyException();
    }
}
