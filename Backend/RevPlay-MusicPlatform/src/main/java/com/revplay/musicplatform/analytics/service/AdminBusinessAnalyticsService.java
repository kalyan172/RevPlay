package com.revplay.musicplatform.analytics.service;

import com.revplay.musicplatform.analytics.dto.response.BusinessOverviewResponse;
import com.revplay.musicplatform.analytics.dto.response.ConversionRateResponse;
import com.revplay.musicplatform.analytics.dto.response.RevenueAnalyticsResponse;
import com.revplay.musicplatform.analytics.dto.response.TopDownloadResponse;
import com.revplay.musicplatform.analytics.dto.response.TopMixResponse;
import java.util.List;

public interface AdminBusinessAnalyticsService {

    BusinessOverviewResponse getBusinessOverview();

    RevenueAnalyticsResponse getRevenueAnalytics();

    List<TopDownloadResponse> getTopDownloadedSongs(int limit);

    List<TopMixResponse> getTopMixes();

    ConversionRateResponse getPremiumConversionRate();
}

