package com.revplay.musicplatform.user.dto.response;

import java.time.Instant;

public record AdminUserDetailsResponse(
        Long id,
        String username,
        String email,
        String role,
        String status,
        Instant createdAt
) {
}
