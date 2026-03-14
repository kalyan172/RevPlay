package com.revplay.musicplatform.artist.dto.response;



import java.time.LocalDateTime;


import com.revplay.musicplatform.artist.enums.ArtistType;
import lombok.Data;

@Data
public class ArtistResponse {
    private Long artistId;
    private Long userId;
    private String displayName;
    private String bio;
    private String bannerImageUrl;
    private ArtistType artistType;
    private Boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

