package com.revplay.musicplatform.playlist.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.playlist.dto.request.CreatePlaylistRequest;
import com.revplay.musicplatform.playlist.entity.Playlist;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PlaylistMapperTest {

    private final PlaylistMapper mapper = new PlaylistMapper();

    @Test
    @DisplayName("toEntity sets default isPublic true when request flag is null")
    void toEntityDefaultsPublic() {
        CreatePlaylistRequest request = new CreatePlaylistRequest();
        request.setName("MyList");
        request.setDescription("desc");
        request.setIsPublic(null);

        Playlist playlist = mapper.toEntity(request, 8L);

        assertThat(playlist.getUserId()).isEqualTo(8L);
        assertThat(playlist.getIsPublic()).isTrue();
    }

    @Test
    @DisplayName("toResponse maps playlist with counts")
    void toResponseMapsFields() {
        Playlist playlist = playlist();
        var response = mapper.toResponse(playlist, 3L, 4L);

        assertThat(response.getSongCount()).isEqualTo(3L);
        assertThat(response.getFollowerCount()).isEqualTo(4L);
    }

    @Test
    @DisplayName("toDetailResponse maps playlist and returns empty songs list")
    void toDetailResponseMapsFields() {
        Playlist playlist = playlist();
        var response = mapper.toDetailResponse(playlist, 5L, 6L);

        assertThat(response.getSongCount()).isEqualTo(5L);
        assertThat(response.getSongs()).isEmpty();
    }

    private Playlist playlist() {
        return Playlist.builder()
                .id(1L)
                .userId(2L)
                .name("Name")
                .description("Desc")
                .isPublic(Boolean.TRUE)
                .createdAt(LocalDateTime.parse("2026-01-01T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-01-02T10:00:00"))
                .build();
    }
}
