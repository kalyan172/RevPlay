package com.revplay.musicplatform.catalog.dto.response;

import java.util.List;

public record DiscoveryFeedResponse(
        Long userId,
        List<NewReleaseItemResponse> newReleases,
        List<TopArtistItemResponse> topArtists,
        List<PopularPodcastItemResponse> popularPodcasts,
        List<DiscoveryRecommendationItemResponse> discoverWeekly
) {
}


