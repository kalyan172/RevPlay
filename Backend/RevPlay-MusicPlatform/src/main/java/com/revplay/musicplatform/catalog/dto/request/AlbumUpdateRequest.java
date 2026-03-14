package com.revplay.musicplatform.catalog.dto.request;



import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlbumUpdateRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 2048)
    private String coverArtUrl;

    private LocalDate releaseDate;
}

