package com.revplay.musicplatform.analytics.dto.response;

public record TopListenerResponse(
        Long userId,
        String username,
        String email,
        String fullName,
        Long playCount
) {
}





