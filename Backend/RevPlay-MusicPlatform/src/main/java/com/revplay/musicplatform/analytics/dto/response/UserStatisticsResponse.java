package com.revplay.musicplatform.analytics.dto.response;

import java.time.Instant;

public record UserStatisticsResponse(
        Long userId,
        Long totalPlaylists,
        Long totalFavoriteSongs,
        Long totalListeningTimeSeconds,
        Long totalSongsPlayed,
        Instant lastUpdated
) {
}





