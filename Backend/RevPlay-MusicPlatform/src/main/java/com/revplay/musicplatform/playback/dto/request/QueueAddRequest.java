package com.revplay.musicplatform.playback.dto.request;

import com.revplay.musicplatform.playback.validation.ValidQueueContentSelection;
import jakarta.validation.constraints.NotNull;

@ValidQueueContentSelection
public record QueueAddRequest(
        @NotNull Long userId,
        Long songId,
        Long episodeId
) {
}










