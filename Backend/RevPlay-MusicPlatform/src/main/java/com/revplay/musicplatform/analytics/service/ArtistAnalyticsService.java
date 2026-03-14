package com.revplay.musicplatform.analytics.service;

import com.revplay.musicplatform.analytics.dto.response.ArtistDashboardResponse;
import com.revplay.musicplatform.analytics.dto.response.ListeningTrendPointResponse;
import com.revplay.musicplatform.analytics.dto.response.SongPopularityResponse;
import com.revplay.musicplatform.analytics.dto.response.TopListenerResponse;
import com.revplay.musicplatform.analytics.enums.TrendRange;
import com.revplay.musicplatform.playback.dto.response.FavoritedUserResponse;
import com.revplay.musicplatform.playback.dto.response.SongPlayCountResponse;

import java.time.LocalDate;
import java.util.List;

public interface ArtistAnalyticsService {

    ArtistDashboardResponse dashboard(Long artistId);

    SongPlayCountResponse songPlayCount(Long artistId, Long songId);

    List<SongPopularityResponse> songPopularity(Long artistId);

    List<FavoritedUserResponse> usersWhoFavoritedSongs(Long artistId);

    List<ListeningTrendPointResponse> listeningTrends(Long artistId, TrendRange range, LocalDate from, LocalDate to);

    List<TopListenerResponse> topListeners(Long artistId, int limit);
}



