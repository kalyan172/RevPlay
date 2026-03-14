package com.revplay.musicplatform.playlist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.playlist.entity.PlaylistSong;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PlaylistSongMapperTest {

    private final PlaylistSongMapper mapper = new PlaylistSongMapper();

    @Test
    @DisplayName("toResponse maps playlist song entity")
    void toResponseMapsFields() {
        PlaylistSong song = PlaylistSong.builder()
                .id(1L)
                .playlistId(2L)
                .songId(3L)
                .position(4)
                .addedAt(LocalDateTime.parse("2026-01-01T00:00:00"))
                .build();

        var response = mapper.toResponse(song);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPosition()).isEqualTo(4);
    }
}
