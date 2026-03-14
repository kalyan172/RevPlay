package com.revplay.musicplatform.catalog.dto.response;

import java.util.List;

public record DiscoverWeeklyResponse(
        Long userId,
        List<DiscoveryRecommendationItemResponse> items
) {
}


