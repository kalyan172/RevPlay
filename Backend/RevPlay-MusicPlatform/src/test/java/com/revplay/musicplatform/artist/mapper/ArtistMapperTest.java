package com.revplay.musicplatform.artist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.artist.dto.request.ArtistCreateRequest;
import com.revplay.musicplatform.artist.dto.request.ArtistUpdateRequest;
import com.revplay.musicplatform.artist.entity.Artist;
import com.revplay.musicplatform.artist.enums.ArtistType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ArtistMapperTest {

    private final ArtistMapper mapper = new ArtistMapper();

    @Test
    @DisplayName("toEntity maps artist create request")
    void toEntityMapsCreateRequest() {
        ArtistCreateRequest request = new ArtistCreateRequest();
        request.setDisplayName("Artist");
        request.setBio("Bio");
        request.setBannerImageUrl("banner");
        request.setArtistType(ArtistType.MUSIC);

        Artist artist = mapper.toEntity(request, 10L);

        assertThat(artist.getUserId()).isEqualTo(10L);
        assertThat(artist.getArtistType()).isEqualTo(ArtistType.MUSIC);
    }

    @Test
    @DisplayName("updateEntity updates artist fields")
    void updateEntityUpdatesFields() {
        Artist artist = new Artist();
        ArtistUpdateRequest request = new ArtistUpdateRequest();
        request.setDisplayName("Updated");
        request.setBio("Updated bio");
        request.setBannerImageUrl("updated-banner");
        request.setArtistType(ArtistType.PODCAST);

        mapper.updateEntity(artist, request);

        assertThat(artist.getDisplayName()).isEqualTo("Updated");
        assertThat(artist.getArtistType()).isEqualTo(ArtistType.PODCAST);
    }

    @Test
    @DisplayName("toResponse maps artist entity")
    void toResponseMapsEntity() {
        Artist artist = new Artist();
        artist.setArtistId(1L);
        artist.setUserId(2L);
        artist.setDisplayName("Name");
        artist.setBio("Bio");
        artist.setBannerImageUrl("img");
        artist.setArtistType(ArtistType.MUSIC);
        artist.setVerified(Boolean.TRUE);
        artist.setCreatedAt(LocalDateTime.parse("2026-01-01T10:00:00"));
        artist.setUpdatedAt(LocalDateTime.parse("2026-01-02T10:00:00"));

        var response = mapper.toResponse(artist);

        assertThat(response.getArtistId()).isEqualTo(1L);
        assertThat(response.getVerified()).isTrue();
    }
}
