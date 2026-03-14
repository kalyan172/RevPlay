package com.revplay.musicplatform.artist.dto.request;



import com.revplay.musicplatform.artist.enums.ArtistType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtistUpdateRequest {
    @NotBlank
    @Size(max = 120)
    private String displayName;

    @Size(max = 1000)
    private String bio;

    @Size(max = 2048)
    private String bannerImageUrl;

    @NotNull
    private ArtistType artistType;
}

