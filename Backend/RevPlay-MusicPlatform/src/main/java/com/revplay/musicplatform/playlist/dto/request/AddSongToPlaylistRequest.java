package com.revplay.musicplatform.playlist.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AddSongToPlaylistRequest {

    @NotNull(message = "Song ID is required")
    @Positive(message = "Song ID must be a positive number")
    private Long songId;

    @Positive(message = "Position must be a positive number")
    private Integer position;
}

