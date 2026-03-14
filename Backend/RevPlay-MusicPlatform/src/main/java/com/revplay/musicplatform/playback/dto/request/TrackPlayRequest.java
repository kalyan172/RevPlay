package com.revplay.musicplatform.playback.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;

public record TrackPlayRequest(
        @NotNull Long userId,
        Long songId,
        Long episodeId,
        Boolean completed,
        @PositiveOrZero Integer playDurationSeconds,
        Instant playedAt
) {
}





