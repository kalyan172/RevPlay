package com.revplay.musicplatform.analytics.service;

import com.revplay.musicplatform.analytics.dto.response.ForYouRecommendationsResponse;
import com.revplay.musicplatform.analytics.dto.response.SongRecommendationResponse;

import java.util.List;

public interface RecommendationService {

    List<SongRecommendationResponse> similarSongs(Long songId, int limit);

    ForYouRecommendationsResponse forUser(Long userId, int limit);
}



