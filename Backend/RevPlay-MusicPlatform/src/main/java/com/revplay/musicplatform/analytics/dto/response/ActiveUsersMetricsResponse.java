package com.revplay.musicplatform.analytics.dto.response;

public record ActiveUsersMetricsResponse(
        Long last24Hours,
        Long last7Days,
        Long last30Days
) {
}





