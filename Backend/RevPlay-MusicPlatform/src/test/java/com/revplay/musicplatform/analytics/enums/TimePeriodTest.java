package com.revplay.musicplatform.analytics.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class TimePeriodTest {

    private static final long DAILY_SECONDS = 86_400L;
    private static final long WEEKLY_SECONDS = 604_800L;
    private static final long MONTHLY_SECONDS = 2_592_000L;
    private static final long TOLERANCE_SECONDS = 5L;

    @ParameterizedTest
    @CsvSource({"daily,DAILY", "WEEKLY,WEEKLY", "Monthly,MONTHLY"})
    @DisplayName("from parses values case-insensitively")
    void fromParsesCaseInsensitiveValues(String value, TimePeriod expected) {
        TimePeriod period = TimePeriod.from(value);

        assertThat(period).isEqualTo(expected);
    }

    @Test
    @DisplayName("from throws validation exception for invalid value")
    void fromThrowsForInvalidValue() {
        assertThatThrownBy(() -> TimePeriod.from("yearly"))
                .isInstanceOf(PlaybackValidationException.class)
                .hasMessage("period must be one of: daily, weekly, monthly");
    }

    @Test
    @DisplayName("sinceNow for DAILY is approximately now minus one day")
    void sinceNowDaily() {
        Instant since = TimePeriod.DAILY.sinceNow();
        long delta = Instant.now().getEpochSecond() - since.getEpochSecond();

        assertThat(delta).isBetween(DAILY_SECONDS - TOLERANCE_SECONDS, DAILY_SECONDS + TOLERANCE_SECONDS);
    }

    @Test
    @DisplayName("sinceNow for WEEKLY is approximately now minus seven days")
    void sinceNowWeekly() {
        Instant since = TimePeriod.WEEKLY.sinceNow();
        long delta = Instant.now().getEpochSecond() - since.getEpochSecond();

        assertThat(delta).isBetween(WEEKLY_SECONDS - TOLERANCE_SECONDS, WEEKLY_SECONDS + TOLERANCE_SECONDS);
    }

    @Test
    @DisplayName("sinceNow for MONTHLY is approximately now minus thirty days")
    void sinceNowMonthly() {
        Instant since = TimePeriod.MONTHLY.sinceNow();
        long delta = Instant.now().getEpochSecond() - since.getEpochSecond();

        assertThat(delta).isBetween(MONTHLY_SECONDS - TOLERANCE_SECONDS, MONTHLY_SECONDS + TOLERANCE_SECONDS);
    }
}
