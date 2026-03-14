package com.revplay.musicplatform.analytics.dto.response;

public record BusinessOverviewResponse(
        long totalUsers,
        long activePremiumUsers,
        long totalAdImpressions,
        long totalDownloads,
        long totalSongPlays
) {
}

