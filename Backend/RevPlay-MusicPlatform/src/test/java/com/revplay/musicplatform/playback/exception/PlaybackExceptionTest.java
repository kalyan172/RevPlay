package com.revplay.musicplatform.playback.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PlaybackExceptionTest {

    private static final String MESSAGE = "playback-error";

    @Test
    @DisplayName("subclass preserves message")
    void subclassPreservesMessage() {
        TestPlaybackException exception = new TestPlaybackException(MESSAGE);

        assertThat(exception).hasMessage(MESSAGE);
    }

    private static final class TestPlaybackException extends PlaybackException {
        private TestPlaybackException(String message) {
            super(message);
        }
    }
}
