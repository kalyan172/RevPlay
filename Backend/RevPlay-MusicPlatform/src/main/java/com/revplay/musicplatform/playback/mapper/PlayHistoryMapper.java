package com.revplay.musicplatform.playback.mapper;

import com.revplay.musicplatform.playback.dto.response.PlayHistoryResponse;
import com.revplay.musicplatform.playback.entity.PlayHistory;
import org.springframework.stereotype.Component;

@Component
public class PlayHistoryMapper {

    public PlayHistoryResponse toDto(PlayHistory entity) {
        return new PlayHistoryResponse(
                entity.getPlayId(),
                entity.getUserId(),
                entity.getSongId(),
                entity.getEpisodeId(),
                entity.getPlayedAt(),
                entity.getCompleted(),
                entity.getPlayDurationSeconds()
        );
    }
}


