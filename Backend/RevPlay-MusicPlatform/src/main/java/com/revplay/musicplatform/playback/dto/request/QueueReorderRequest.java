package com.revplay.musicplatform.playback.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QueueReorderRequest(
        @NotNull Long userId,
        @NotEmpty List<@NotNull Long> queueIdsInOrder
) {
}





