package com.revplay.musicplatform.playlist.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LikeRequest {

    @NotNull(message = "Likeable ID is required")
    @Positive(message = "Likeable ID must be a positive number")
    private Long likeableId;


    @NotNull(message = "Likeable type is required")
    @Pattern(regexp = "SONG|PODCAST", message = "Likeable type must be SONG or PODCAST")
    private String likeableType;

}
