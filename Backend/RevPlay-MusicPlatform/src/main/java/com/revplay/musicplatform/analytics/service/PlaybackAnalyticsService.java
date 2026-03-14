package com.revplay.musicplatform.analytics.service;

import com.revplay.musicplatform.analytics.dto.response.DashboardMetricsResponse;
import com.revplay.musicplatform.analytics.dto.response.TopArtistResponse;
import com.revplay.musicplatform.analytics.dto.response.TrendingContentResponse;
import com.revplay.musicplatform.analytics.dto.response.UserListeningStatsResponse;
import com.revplay.musicplatform.analytics.enums.TimePeriod;

import java.util.List;

public interface PlaybackAnalyticsService {

    List<TrendingContentResponse> trending(String type, TimePeriod period, int limit);

    List<TopArtistResponse> topArtists(int limit);

    List<TrendingContentResponse> topContent(String type, int limit);

    UserListeningStatsResponse userStats(Long userId);

    DashboardMetricsResponse dashboardMetrics();
}



