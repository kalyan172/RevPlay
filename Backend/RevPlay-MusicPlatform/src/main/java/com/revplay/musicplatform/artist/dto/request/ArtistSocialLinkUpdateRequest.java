package com.revplay.musicplatform.artist.dto.request;

import com.revplay.musicplatform.catalog.enums.SocialPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtistSocialLinkUpdateRequest {
    @NotNull
    private SocialPlatform platform;

    @NotBlank
    @Size(max = 2048)
    private String url;
}
