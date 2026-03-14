package com.revplay.musicplatform.playlist.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class ReorderPlaylistSongsRequest {

    @NotEmpty(message = "Song positions list must not be empty")
    @Valid
    private List<SongPositionRequest> songs;
}
