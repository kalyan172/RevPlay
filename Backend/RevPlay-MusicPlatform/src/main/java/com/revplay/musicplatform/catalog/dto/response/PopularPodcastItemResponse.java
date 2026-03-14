package com.revplay.musicplatform.catalog.dto.response;

public record PopularPodcastItemResponse(
        Long podcastId,
        String title,
        Long playCount
) {
}


