package com.revplay.musicplatform.catalog.dto.response;

public record GenreResponse(
        Long genreId,
        String name,
        String description,
        Boolean isActive
) {
}

