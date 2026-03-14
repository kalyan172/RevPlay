package com.revplay.musicplatform.analytics.enums;

import com.revplay.musicplatform.playback.exception.PlaybackValidationException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public enum TimePeriod {
    DAILY,
    WEEKLY,
    MONTHLY;

    public static TimePeriod from(String value) {
        try {
            return TimePeriod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PlaybackValidationException("period must be one of: daily, weekly, monthly");
        }
    }

    public Instant sinceNow() {
        return switch (this) {
            case DAILY -> Instant.now().minus(1, ChronoUnit.DAYS);
            case WEEKLY -> Instant.now().minus(7, ChronoUnit.DAYS);
            case MONTHLY -> Instant.now().minus(30, ChronoUnit.DAYS);
        };
    }
}




