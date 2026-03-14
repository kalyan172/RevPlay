package com.revplay.musicplatform.playback.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.playback.dto.request.QueueAddRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class QueueContentSelectionValidatorTest {

    private static final Long USER_ID = 1L;

    private final QueueContentSelectionValidator validator = new QueueContentSelectionValidator();

    @Test
    @DisplayName("validator returns false when both song and episode are null")
    void bothNullInvalid() {
        boolean valid = validator.isValid(new QueueAddRequest(USER_ID, null, null), null);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validator returns false when both song and episode are set")
    void bothSetInvalid() {
        boolean valid = validator.isValid(new QueueAddRequest(USER_ID, 10L, 20L), null);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("validator returns true when only song id is set")
    void onlySongValid() {
        boolean valid = validator.isValid(new QueueAddRequest(USER_ID, 10L, null), null);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("validator returns true when only episode id is set")
    void onlyEpisodeValid() {
        boolean valid = validator.isValid(new QueueAddRequest(USER_ID, null, 20L), null);
        assertThat(valid).isTrue();
    }
}
