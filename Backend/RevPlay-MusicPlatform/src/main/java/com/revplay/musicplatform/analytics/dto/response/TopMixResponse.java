package com.revplay.musicplatform.analytics.dto.response;

public record TopMixResponse(
        String playlistName,
        Long totalPlayCount
) {
}

