package com.revplay.musicplatform.catalog.dto.request;


import java.time.LocalDate;

import com.revplay.musicplatform.catalog.enums.ContentVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SongCreateRequest {
    private Long albumId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    @Positive
    private Integer durationSeconds;

    private ContentVisibility visibility;

    private LocalDate releaseDate;
}
