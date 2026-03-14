package com.revplay.musicplatform.analytics.enums;

import com.revplay.musicplatform.playback.exception.PlaybackValidationException;

public enum TrendRange {
    DAILY,
    WEEKLY,
    MONTHLY;

    public static TrendRange from(String value) {
        try {
            return TrendRange.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PlaybackValidationException("range must be one of: daily, weekly, monthly");
        }
    }
}




