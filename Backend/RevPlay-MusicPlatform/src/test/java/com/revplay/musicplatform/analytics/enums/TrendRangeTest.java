package com.revplay.musicplatform.analytics.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class TrendRangeTest {

    @ParameterizedTest
    @CsvSource({"daily,DAILY", "WEEKLY,WEEKLY", "Monthly,MONTHLY"})
    @DisplayName("from parses values case-insensitively")
    void fromParsesCaseInsensitiveValues(String value, TrendRange expected) {
        TrendRange range = TrendRange.from(value);

        assertThat(range).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"yearly", "invalid"})
    @DisplayName("from throws validation exception for invalid values")
    void fromThrowsForInvalidValues(String value) {
        assertThatThrownBy(() -> TrendRange.from(value))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("range must be one of: daily, weekly, monthly");
    }
}
