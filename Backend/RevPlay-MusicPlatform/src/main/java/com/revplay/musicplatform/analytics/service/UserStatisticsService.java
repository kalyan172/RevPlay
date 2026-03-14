package com.revplay.musicplatform.analytics.service;

import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;

public interface UserStatisticsService {

    UserStatisticsResponse getByUserId(Long userId);

    UserStatisticsResponse refreshAndGet(Long userId);
}



