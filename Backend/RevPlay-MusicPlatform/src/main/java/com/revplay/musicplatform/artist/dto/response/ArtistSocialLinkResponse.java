package com.revplay.musicplatform.artist.dto.response;





import com.revplay.musicplatform.catalog.enums.SocialPlatform;
import lombok.Data;

@Data
public class ArtistSocialLinkResponse {
    private Long linkId;
    private Long artistId;
    private SocialPlatform platform;
    private String url;
}

