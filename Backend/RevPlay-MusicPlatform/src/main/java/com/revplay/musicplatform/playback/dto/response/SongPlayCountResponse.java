package com.revplay.musicplatform.playback.dto.response;

public record SongPlayCountResponse(
        Long songId,
        String title,
        Long playCount
) {
}





