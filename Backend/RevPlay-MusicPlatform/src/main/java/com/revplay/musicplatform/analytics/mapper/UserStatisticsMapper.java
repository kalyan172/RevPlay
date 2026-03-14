package com.revplay.musicplatform.analytics.mapper;

import com.revplay.musicplatform.analytics.dto.response.UserStatisticsResponse;
import com.revplay.musicplatform.analytics.entity.UserStatistics;
import org.springframework.stereotype.Component;

@Component
public class UserStatisticsMapper {

    public UserStatisticsResponse toDto(UserStatistics stats) {
        return new UserStatisticsResponse(
                stats.getUserId(),
                stats.getTotalPlaylists(),
                stats.getTotalFavoriteSongs(),
                stats.getTotalListeningTimeSeconds(),
                stats.getTotalSongsPlayed(),
                stats.getLastUpdated()
        );
    }
}


