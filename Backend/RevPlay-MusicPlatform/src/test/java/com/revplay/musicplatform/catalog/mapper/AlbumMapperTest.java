package com.revplay.musicplatform.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.catalog.dto.request.AlbumCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.AlbumUpdateRequest;
import com.revplay.musicplatform.catalog.entity.Album;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AlbumMapperTest {

    private final AlbumMapper mapper = new AlbumMapper();

    @Test
    @DisplayName("toEntity maps album create request")
    void toEntityMapsCreateRequest() {
        AlbumCreateRequest request = new AlbumCreateRequest();
        request.setTitle("Album A");
        request.setDescription("desc");
        request.setCoverArtUrl("cover.png");
        request.setReleaseDate(LocalDate.parse("2026-01-01"));

        Album album = mapper.toEntity(request, 5L);

        assertThat(album.getArtistId()).isEqualTo(5L);
        assertThat(album.getTitle()).isEqualTo("Album A");
    }

    @Test
    @DisplayName("updateEntity updates album fields")
    void updateEntityUpdatesFields() {
        Album album = new Album();
        AlbumUpdateRequest request = new AlbumUpdateRequest();
        request.setTitle("Updated");
        request.setDescription("updated-desc");
        request.setCoverArtUrl("new.png");
        request.setReleaseDate(LocalDate.parse("2026-02-02"));

        mapper.updateEntity(album, request);

        assertThat(album.getTitle()).isEqualTo("Updated");
        assertThat(album.getCoverArtUrl()).isEqualTo("new.png");
    }

    @Test
    @DisplayName("toResponse maps album entity")
    void toResponseMapsEntity() {
        Album album = new Album();
        album.setAlbumId(1L);
        album.setArtistId(2L);
        album.setTitle("T");
        album.setDescription("D");
        album.setCoverArtUrl("C");
        album.setReleaseDate(LocalDate.parse("2026-03-03"));

        var response = mapper.toResponse(album);

        assertThat(response.getAlbumId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("T");
    }
}
