package com.revplay.musicplatform.playback.dto.response;

import java.time.Instant;

public record QueueItemResponse(
        Long queueId,
        Long userId,
        Long songId,
        Long episodeId,
        Integer position,
        Instant createdAt
) {
}





