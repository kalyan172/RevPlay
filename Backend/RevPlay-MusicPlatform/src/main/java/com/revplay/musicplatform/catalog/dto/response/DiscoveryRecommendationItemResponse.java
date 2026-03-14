package com.revplay.musicplatform.catalog.dto.response;

public record DiscoveryRecommendationItemResponse(
        Long songId,
        String title,
        Long artistId,
        String artistName,
        Long score
) {
}


