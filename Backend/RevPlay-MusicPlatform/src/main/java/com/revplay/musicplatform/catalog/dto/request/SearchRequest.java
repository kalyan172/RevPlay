package com.revplay.musicplatform.catalog.dto.request;

import com.revplay.musicplatform.catalog.enums.SearchContentType;
import java.time.LocalDate;

public record SearchRequest(
        String query,
        SearchContentType type,
        Long genreId,
        LocalDate releaseDateFrom,
        LocalDate releaseDateTo,
        String artistType,
        int page,
        int size,
        String sortBy,
        String sortDir
) {
}


