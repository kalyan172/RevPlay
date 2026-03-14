package com.revplay.musicplatform.user.dto.response;

import java.time.Instant;

public record UserResponse(
        Long userId,
        String email,
        String username,
        String role,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
