package com.revplay.musicplatform.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull Boolean isActive
) {
}
