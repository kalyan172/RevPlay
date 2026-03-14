package com.revplay.musicplatform.playback.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class PlaybackValidationUtilTest {

    @Test
    @DisplayName("requireExactlyOneContentId rejects both null")
    void requireExactlyOneContentIdBothNull() {
        assertThatThrownBy(() -> PlaybackValidationUtil.requireExactlyOneContentId(null, null))
                .isInstanceOf(PlaybackValidationException.class);
    }

    @Test
    @DisplayName("requireExactlyOneContentId rejects both provided")
    void requireExactlyOneContentIdBothSet() {
        assertThatThrownBy(() -> PlaybackValidationUtil.requireExactlyOneContentId(1L, 2L))
                .isInstanceOf(PlaybackValidationException.class);
    }

    @Test
    @DisplayName("requireExactlyOneContentId accepts song only")
    void requireExactlyOneContentIdSongOnly() {
        assertThatCode(() -> PlaybackValidationUtil.requireExactlyOneContentId(1L, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("requireExactlyOneContentId accepts episode only")
    void requireExactlyOneContentIdEpisodeOnly() {
        assertThatCode(() -> PlaybackValidationUtil.requireExactlyOneContentId(null, 2L))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"0,1,100", "101,1,100"})
    @DisplayName("requireLimitInRange rejects values out of range")
    void requireLimitInRangeRejectsOutOfRange(int limit, int min, int max) {
        assertThatThrownBy(() -> PlaybackValidationUtil.requireLimitInRange(limit, min, max))
                .isInstanceOf(PlaybackValidationException.class);
    }

    @Test
    @DisplayName("requireLimitInRange accepts in range values")
    void requireLimitInRangeAcceptsInRange() {
        assertThatCode(() -> PlaybackValidationUtil.requireLimitInRange(50, 1, 100))
                .doesNotThrowAnyException();
    }
}
