package com.revplay.musicplatform.artist.dto.request;


import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArtistVerifyRequest {
    @NotNull
    private Boolean verified;
}

