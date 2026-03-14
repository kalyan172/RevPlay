package com.revplay.musicplatform.playlist.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class SongPositionRequest {

    @NotNull(message = "Song ID is required")
    @Positive(message = "Song ID must be a positive number")
    private Long songId;


    @NotNull(message = "Position is required")
    @Positive(message = "Position must be a positive number")
    private Integer position;
}
