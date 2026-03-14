package com.revplay.musicplatform.catalog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreUpsertRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,
        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description
) {
}

