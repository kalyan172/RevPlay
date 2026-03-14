package com.revplay.musicplatform.playback.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PlaybackValidationExceptionTest {

    private static final String MESSAGE = "validation-error";

    @Test
    @DisplayName("constructor sets message")
    void constructorSetsMessage() {
        PlaybackValidationException exception = new PlaybackValidationException(MESSAGE);

        assertThat(exception).hasMessage(MESSAGE);
    }
}
