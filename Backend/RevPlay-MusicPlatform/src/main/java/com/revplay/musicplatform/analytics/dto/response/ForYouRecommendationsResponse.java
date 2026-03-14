package com.revplay.musicplatform.analytics.dto.response;

import java.util.List;

public record ForYouRecommendationsResponse(
        Long userId,
        List<SongRecommendationResponse> youMightLike,
        List<SongRecommendationResponse> popularWithSimilarUsers
) {
}





