package com.revplay.musicplatform.catalog.service;

import com.revplay.musicplatform.catalog.dto.response.DiscoverWeeklyResponse;
import com.revplay.musicplatform.catalog.dto.response.DiscoveryFeedResponse;

public interface DiscoveryFeedService {

    DiscoverWeeklyResponse discoverWeekly(Long userId, int limit);

    DiscoveryFeedResponse homeFeed(Long userId, int sectionLimit);
}



