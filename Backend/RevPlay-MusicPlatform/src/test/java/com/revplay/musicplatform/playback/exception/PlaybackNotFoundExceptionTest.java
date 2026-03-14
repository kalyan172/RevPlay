package com.revplay.musicplatform.playback.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PlaybackNotFoundExceptionTest {

    private static final String MESSAGE = "not-found";

    @Test
    @DisplayName("constructor sets message")
    void constructorSetsMessage() {
        PlaybackNotFoundException exception = new PlaybackNotFoundException(MESSAGE);

        assertThat(exception).hasMessage(MESSAGE);
    }
}
