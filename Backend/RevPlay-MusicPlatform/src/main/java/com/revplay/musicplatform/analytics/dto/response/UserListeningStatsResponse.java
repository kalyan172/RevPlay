package com.revplay.musicplatform.analytics.dto.response;

import java.util.List;

public record UserListeningStatsResponse(
        UserStatisticsResponse baseStatistics,
        List<GenrePlayCountResponse> topGenres,
        Integer peakListeningHour
) {
}




