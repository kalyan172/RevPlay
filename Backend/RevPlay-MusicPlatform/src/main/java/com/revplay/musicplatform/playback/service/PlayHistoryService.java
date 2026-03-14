package com.revplay.musicplatform.playback.service;

import com.revplay.musicplatform.playback.dto.request.TrackPlayRequest;
import com.revplay.musicplatform.playback.dto.response.PlayHistoryResponse;

import java.util.List;

public interface PlayHistoryService {

    void trackPlay(TrackPlayRequest request);

    List<PlayHistoryResponse> getHistory(Long userId);

    List<PlayHistoryResponse> recentlyPlayed(Long userId);

    long clearHistory(Long userId);
}



