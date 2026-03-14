package com.revplay.musicplatform.analytics.dto.response;

public record SongRecommendationResponse(
        Long songId,
        String title,
        Long artistId,
        String artistName,
        Long score
) {
}





