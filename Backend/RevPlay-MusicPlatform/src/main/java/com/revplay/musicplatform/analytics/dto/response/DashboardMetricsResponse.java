package com.revplay.musicplatform.analytics.dto.response;

public record DashboardMetricsResponse(
        Long totalPlatformPlays,
        Long playsLast24Hours,
        ActiveUsersMetricsResponse activeUsers,
        ContentPerformanceResponse contentPerformance
) {
}





