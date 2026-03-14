package com.revplay.musicplatform.analytics.dto.response;

public record RevenueAnalyticsResponse(
        double monthlyRevenue,
        double yearlyRevenue,
        double totalRevenue
) {
}

