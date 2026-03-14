package com.revplay.musicplatform.playback.mapper;

import com.revplay.musicplatform.playback.dto.response.QueueItemResponse;
import com.revplay.musicplatform.playback.entity.QueueItem;
import org.springframework.stereotype.Component;

@Component
public class QueueItemMapper {

    public QueueItemResponse toDto(QueueItem entity) {
        return new QueueItemResponse(
                entity.getQueueId(),
                entity.getUserId(),
                entity.getSongId(),
                entity.getEpisodeId(),
                entity.getPosition(),
                entity.getCreatedAt()
        );
    }
}


