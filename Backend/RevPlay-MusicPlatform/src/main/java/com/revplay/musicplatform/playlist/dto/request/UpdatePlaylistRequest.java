package com.revplay.musicplatform.playlist.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UpdatePlaylistRequest {

    @Size(min = 1, max = 100, message = "Playlist name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean isPublic;
}
