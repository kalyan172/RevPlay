package com.revplay.musicplatform.analytics.dto.response;

public record SongPopularityResponse(
        Long songId,
        String title,
        Long playCount,
        Long favoriteCount
) {
}





