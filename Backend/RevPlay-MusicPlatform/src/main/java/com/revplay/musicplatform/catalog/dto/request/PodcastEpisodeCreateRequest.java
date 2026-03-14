package com.revplay.musicplatform.catalog.dto.request;


import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PodcastEpisodeCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    @Positive
    private Integer durationSeconds;

    private LocalDate releaseDate;
}

