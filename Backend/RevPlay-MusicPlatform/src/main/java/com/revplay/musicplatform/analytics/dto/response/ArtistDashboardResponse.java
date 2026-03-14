package com.revplay.musicplatform.analytics.dto.response;

public record ArtistDashboardResponse(
        Long artistId,
        Long totalSongs,
        Long totalPlays,
        Long totalFavorites
) {
}





