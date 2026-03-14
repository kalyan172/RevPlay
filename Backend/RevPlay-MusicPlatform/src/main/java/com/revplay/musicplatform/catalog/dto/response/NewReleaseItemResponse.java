package com.revplay.musicplatform.catalog.dto.response;

import java.time.LocalDate;

public record NewReleaseItemResponse(
        String type,
        Long contentId,
        String title,
        Long artistId,
        String artistName,
        LocalDate releaseDate
) {
}


