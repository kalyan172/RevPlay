package com.revplay.musicplatform.artist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistSocialLinkUpdateRequest;
import com.revplay.musicplatform.artist.entity.ArtistSocialLink;
import com.revplay.musicplatform.catalog.enums.SocialPlatform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ArtistSocialLinkMapperTest {

    private final ArtistSocialLinkMapper mapper = new ArtistSocialLinkMapper();

    @Test
    @DisplayName("toEntity maps social link create request")
    void toEntityMapsCreateRequest() {
        ArtistSocialLinkCreateRequest request = new ArtistSocialLinkCreateRequest();
        request.setPlatform(SocialPlatform.INSTAGRAM);
        request.setUrl("https://instagram.com/a");

        ArtistSocialLink link = mapper.toEntity(request, 7L);

        assertThat(link.getArtistId()).isEqualTo(7L);
        assertThat(link.getPlatform()).isEqualTo(SocialPlatform.INSTAGRAM);
    }

    @Test
    @DisplayName("updateEntity updates social link fields")
    void updateEntityUpdatesFields() {
        ArtistSocialLink link = new ArtistSocialLink();
        ArtistSocialLinkUpdateRequest request = new ArtistSocialLinkUpdateRequest();
        request.setPlatform(SocialPlatform.YOUTUBE);
        request.setUrl("https://youtube.com/a");

        mapper.updateEntity(link, request);

        assertThat(link.getPlatform()).isEqualTo(SocialPlatform.YOUTUBE);
        assertThat(link.getUrl()).isEqualTo("https://youtube.com/a");
    }

    @Test
    @DisplayName("toResponse maps social link entity")
    void toResponseMapsEntity() {
        ArtistSocialLink link = new ArtistSocialLink();
        link.setLinkId(1L);
        link.setArtistId(2L);
        link.setPlatform(SocialPlatform.TWITTER);
        link.setUrl("https://x.com/a");

        var response = mapper.toResponse(link);

        assertThat(response.getLinkId()).isEqualTo(1L);
        assertThat(response.getPlatform()).isEqualTo(SocialPlatform.TWITTER);
    }
}
