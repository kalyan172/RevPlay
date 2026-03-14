package com.revplay.musicplatform.catalog.dto.response;

public record TopArtistItemResponse(
        Long artistId,
        String displayName,
        String artistType,
        Long playCount
) {
}


