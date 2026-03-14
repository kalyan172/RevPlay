package com.revplay.musicplatform.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.catalog.dto.request.SongCreateRequest;
import com.revplay.musicplatform.catalog.dto.request.SongUpdateRequest;
import com.revplay.musicplatform.catalog.entity.Song;
import com.revplay.musicplatform.catalog.enums.ContentVisibility;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class SongMapperTest {

    private final SongMapper mapper = new SongMapper();

    @Test
    @DisplayName("toEntity maps song create request fields")
    void toEntityMapsFields() {
        SongCreateRequest request = new SongCreateRequest();
        request.setAlbumId(9L);
        request.setTitle("Song A");
        request.setDurationSeconds(180);
        request.setReleaseDate(LocalDate.parse("2026-01-01"));
        request.setVisibility(ContentVisibility.PUBLIC);

        Song song = mapper.toEntity(request, 2L, "song.mp3");

        assertThat(song.getArtistId()).isEqualTo(2L);
        assertThat(song.getAlbumId()).isEqualTo(9L);
        assertThat(song.getFileUrl()).isEqualTo("song.mp3");
    }

    @Test
    @DisplayName("updateEntity updates mutable song fields")
    void updateEntityUpdatesFields() {
        Song song = new Song();
        SongUpdateRequest request = new SongUpdateRequest();
        request.setTitle("Updated");
        request.setDurationSeconds(200);
        request.setAlbumId(7L);
        request.setReleaseDate(LocalDate.parse("2026-02-02"));

        mapper.updateEntity(song, request);

        assertThat(song.getTitle()).isEqualTo("Updated");
        assertThat(song.getDurationSeconds()).isEqualTo(200);
        assertThat(song.getAlbumId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("toResponse maps entity fields to response")
    void toResponseMapsFields() {
        Song song = new Song();
        song.setSongId(1L);
        song.setArtistId(2L);
        song.setAlbumId(3L);
        song.setTitle("Title");
        song.setDurationSeconds(120);
        song.setFileUrl("f.mp3");
        song.setVisibility(ContentVisibility.UNLISTED);
        song.setReleaseDate(LocalDate.parse("2026-03-03"));
        song.setIsActive(Boolean.TRUE);
        song.setCreatedAt(LocalDateTime.parse("2026-03-03T10:15:30"));

        var response = mapper.toResponse(song);

        assertThat(response.getSongId()).isEqualTo(1L);
        assertThat(response.getVisibility()).isEqualTo(ContentVisibility.UNLISTED);
        assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-03-03T10:15:30"));
    }
}
