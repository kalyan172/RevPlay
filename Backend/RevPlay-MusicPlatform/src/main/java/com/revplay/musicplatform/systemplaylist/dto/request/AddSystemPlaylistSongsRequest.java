package com.revplay.musicplatform.systemplaylist.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddSystemPlaylistSongsRequest(
        @NotEmpty(message = "songIds must not be empty")
        List<@NotNull(message = "songId cannot be null") Long> songIds
) {
}

