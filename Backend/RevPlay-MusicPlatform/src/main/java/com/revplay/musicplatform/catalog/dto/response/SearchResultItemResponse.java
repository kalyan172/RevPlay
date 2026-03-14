package com.revplay.musicplatform.catalog.dto.response;

import java.time.LocalDate;

public record SearchResultItemResponse(
        String type,
        Long contentId,
        String title,
        Long artistId,
        String artistName,
        String artistType,
        LocalDate releaseDate
) {
}


