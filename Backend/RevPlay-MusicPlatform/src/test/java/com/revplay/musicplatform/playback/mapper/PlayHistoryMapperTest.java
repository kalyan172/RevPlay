package com.revplay.musicplatform.playback.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.revplay.musicplatform.playback.entity.PlayHistory;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class PlayHistoryMapperTest {

    private final PlayHistoryMapper mapper = new PlayHistoryMapper();

    @Test
    @DisplayName("toDto maps play history entity fields")
    void toDtoMapsFields() {
        PlayHistory entity = new PlayHistory();
        entity.setPlayId(1L);
        entity.setUserId(2L);
        entity.setSongId(3L);
        entity.setEpisodeId(null);
        entity.setPlayedAt(Instant.parse("2026-01-01T00:00:00Z"));
        entity.setCompleted(Boolean.TRUE);
        entity.setPlayDurationSeconds(100);

        var dto = mapper.toDto(entity);

        assertThat(dto.playId()).isEqualTo(1L);
        assertThat(dto.completed()).isTrue();
        assertThat(dto.playDurationSeconds()).isEqualTo(100);
    }
}
