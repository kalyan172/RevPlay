package com.revplay.musicplatform.security;

import com.revplay.musicplatform.user.enums.UserRole;

public record AuthenticatedUserPrincipal(
        Long userId,
        String username,
        UserRole role
) {
}
