package com.revplay.musicplatform.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        String bio,
        String profilePictureUrl,
        String country
) {
}
