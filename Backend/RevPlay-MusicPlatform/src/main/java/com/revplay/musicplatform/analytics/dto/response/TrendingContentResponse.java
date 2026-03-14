package com.revplay.musicplatform.analytics.dto.response;

public record TrendingContentResponse(
        String type,
        Long contentId,
        String title,
        Long playCount
) {
}





